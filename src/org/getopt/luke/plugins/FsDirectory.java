/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.getopt.luke.plugins;


import java.io.*;
import java.util.Collection;
import java.util.Random;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.lucene.store.*;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Reads a Lucene index stored in DFS. This is a modified version of a class
 * with the same purpose found in org.apache.nutch.indexer. */
public class FsDirectory extends BaseDirectory {
  private static final Logger LOG = LoggerFactory.getLogger(FsDirectory.class);

  private FileSystem fs;
  private Path directory;
  private int ioFileBufferSize;
  
  public static class NullReporter implements IOReporter {
    @Override
    public void reportIO(String name, long bytes, boolean read) { }
    @Override
    public void reportStatus(String msg) { }
  }
  
  public static final IOReporter NULL_REPORTER = new NullReporter();

  private IOReporter reporter;
  Cache cache;
      
  public FsDirectory(FileSystem fs, Path directory, boolean create, Configuration conf,
          IOReporter reporter, int bufSize)
    throws IOException {
    super(NoLockFactory.INSTANCE);
    this.fs = fs;
    this.directory = directory;
    this.reporter = reporter;
    this.ioFileBufferSize = bufSize;
    String diskCacheDir = new File(conf.get("hadoop.tmp.dir"), "fsdir-cache").toString();
    LogManager.getLogger(FsDirectory.class).setLevel(Level.WARN);
    CacheManager mgr = CacheManager.create();
    synchronized(mgr) {
      cache = mgr.getCache("dfsin");
      if (cache == null) {
        cache = new Cache("dfsin", 2000, MemoryStoreEvictionPolicy.LFU,
                true, diskCacheDir, false, 1800, 600, false, 300, null);
        mgr.addCache(cache);
        cache = mgr.getCache("dfsin");
        LOG.info("Created cache: " + cache.toString());
      }
    }
    if (create) {
      create();
    }

    if (!fs.getFileStatus(directory).isDir())
      throw new IOException(directory + " not a directory");
  }

  private void create() throws IOException {
    if (!fs.exists(directory)) {
      fs.mkdirs(directory);
      reporter.reportStatus("Created " + directory);
    }

    if (!fs.getFileStatus(directory).isDir())
      throw new IOException(directory + " not a directory");

    // clear old files
    FileStatus[] fstats = fs.listStatus(directory);
    Path[] files = FileUtil.stat2Paths(fstats);
    if (files.length > 0) {
      reporter.reportStatus("Cleaning " + files.length + " old files.");
    }
    for (int i = 0; i < files.length; i++) {
      if (!fs.delete(files[i], false))
        throw new IOException("Cannot delete " + files[i]);
    }
  }

  public String[] listAll() throws IOException {
    FileStatus[] fstats = fs.listStatus(directory);
    Path[] files = FileUtil.stat2Paths(fstats);
    if (files == null) return null;

    String[] result = new String[files.length];
    for (int i = 0; i < files.length; i++) {
      result[i] = files[i].getName();
    }
    return result;
  }

  public long fileModified(String name) throws IOException {
    return fs.getFileStatus(new Path(directory, name)).getModificationTime();
  }

  public void touchFile(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public long fileLength(String name) throws IOException {
    return fs.getFileStatus(new Path(directory, name)).getLen();
  }

  @Override
  public void deleteFile(String name) throws IOException {
    if (!fs.delete(new Path(directory, name), false))
      throw new IOException("Cannot delete " + name);
  }

  @Override
  public void renameFile(String from, String to) throws IOException {
    // DFS is currently broken when target already exists,
    // so we explicitly delete the target first.
    Path target = new Path(directory, to);
    if (fs.exists(target)) {
      fs.delete(target, false);
    }
    fs.rename(new Path(directory, from), target);
  }

  @Override
  public IndexOutput createOutput(String name, IOContext ctx) throws IOException {
    Path file = new Path(directory, name);
    if (fs.exists(file) && !fs.delete(file, false))      // delete existing, if any
      throw new IOException("Cannot overwrite: " + file);

    return new DfsIndexOutput(file, this.ioFileBufferSize);
  }


  @Override
  public IndexInput openInput(String name, IOContext ctx) throws IOException {
    int bufSize = this.ioFileBufferSize;
    if (name.endsWith(".nrm")) { // need to read all data
      bufSize += bufSize;
    } else if (name.endsWith(".tii")) {
      if (bufSize < 65536) bufSize = 65536;
    }
    return new DfsIndexInput(new Path(directory, name), bufSize, reporter);
  }

  @Override
  public synchronized void close() throws IOException {
    fs.close();
  }

  @Override
  public String toString() {
    return this.getClass().getName() + "@" + directory;
  }

  private class DfsIndexInput extends BufferedIndexInput {

    /** Shared by clones. */
    private class Descriptor {
      public FSDataInputStream in;
      public long position;                       // cache of in.getPos()
      public Descriptor(Path file, int ioFileBufferSize) throws IOException {
        this.in = fs.open(file);
      }
    }

    private final Descriptor descriptor;
    private final long length;
    private boolean isClone;
    private final IOReporter reporter;
    private final String name;
    private final String keyPrefix;

    public DfsIndexInput(Path path, int ioFileBufferSize, IOReporter reporter) throws IOException {
      super(path.getName(), ioFileBufferSize);
      descriptor = new Descriptor(path,ioFileBufferSize);
      length = fs.getFileStatus(path).getLen();
      this.reporter = reporter;
      this.name = path.getName();
      keyPrefix = path.toString() + ":";
    }

    @Override
    protected void readInternal(byte[] b, int offset, int len)
      throws IOException {
      synchronized (descriptor) {
        long position = getFilePointer();
        // check cache
        String key = keyPrefix + position + "+" + len;
        //LOG.info("check key=" + key);
        Element el = cache.get(key);
        if (el != null) {
          LOG.info(cache.getStatistics().toString());
          //LOG.info("cache hit, key=" + key);
          byte[] data = (byte[])el.getObjectValue();
          assert data.length == len;
          System.arraycopy(data, 0, b, offset, len);
          reporter.reportIO(name, len, false);
          return;
        }
        //LOG.info("cache miss, key=" + key);
        if (position != descriptor.position) {
          //reporter.reportStatus("seek " + name + ": " + descriptor.position + " -> " + position
          //        + " (delta " + (position - descriptor.position) + ")");
          descriptor.in.seek(position);
          descriptor.position = position;
        }
        int total = 0;
        do {
          int i = descriptor.in.read(b, offset+total, len-total);
          reporter.reportIO(name, i, true);
          if (i == -1)
            throw new IOException("read past EOF");
          descriptor.position += i;
          total += i;
        } while (total < len);
        // add this data to cache
        byte[] data = new byte[len];
        System.arraycopy(b, offset, data, 0, len);
        cache.put(new Element(key, data));
      }
    }

    @Override
    public void close() throws IOException {
      if (!isClone) {
        descriptor.in.close();
      }
    }

    @Override
    protected void seekInternal(long position) {} // handled in readInternal()

    @Override
    public long length() {
      return length;
    }

    @Override
    protected void finalize() throws IOException {
        try {
            close();                                      // close the file
        } finally {
            try {
                super.finalize();
            } catch (Throwable ex) {
                java.util.logging.Logger.getLogger(FsDirectory.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
                throw new IOException(ex);
            }
        }
    }

    @Override
    public BufferedIndexInput clone() {
      DfsIndexInput clone = (DfsIndexInput)super.clone();
      clone.isClone = true;
      return clone;
    }
  }

  private class DfsIndexOutput extends OutputStreamIndexOutput {
    private FSDataOutputStream out;
    private RandomAccessFile local;
    private File localFile;

    public DfsIndexOutput(Path path, int ioFileBufferSize) throws IOException {
      super(path.getName(),new FileOutputStream(new File(path.toUri())), ioFileBufferSize);
      // create a temporary local file and set it to delete on exit
      String randStr = Integer.toString(new Random().nextInt(Integer.MAX_VALUE));
      localFile = File.createTempFile("index_" + randStr, ".tmp");
      localFile.deleteOnExit();
      local = new RandomAccessFile(localFile, "rw");

      out = fs.create(path);
    }

    @Override
    public void close() throws IOException {            
      super.close();
      
      // transfer to dfs from local
      byte[] buffer = new byte[4096];
      local.seek(0);
      int read;
      while ((read = local.read(buffer)) != -1) {
        out.write(buffer, 0, read);
      }
      out.close();
      local.close();
    }

    public void seek(long pos) throws IOException {      
      local.seek(pos);
    }        
  }

  @Override
  public void sync(Collection<String> names) throws IOException {
    // not easily supported... we would have to track all open outputs
  }

}
