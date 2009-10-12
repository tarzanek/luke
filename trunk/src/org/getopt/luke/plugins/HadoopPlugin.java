package org.getopt.luke.plugins;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.StringUtils;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.getopt.luke.IntPair;
import org.getopt.luke.LukePlugin;
import org.getopt.luke.SlowThread;

public class HadoopPlugin extends LukePlugin {
  
  private String lastMsg = "?";
  private Object ioTable;
  private Object btOpen;
  private Object bar;
  private Object status;
  private Object indexUri;
  private Object total;
  private Object bufSize;
  private IndexReader myIr = null;
  private int parts = 0;
  private int bufferSize = 4096;

  @Override
  public String getPluginHome() {
    return "mailto:ab@getopt.org";
  }

  @Override
  public String getPluginInfo() {
    return "Open indexes located on any filesystem supported by Hadoop.";
  }

  @Override
  public String getPluginName() {
    return "Hadoop Plugin";
  }

  @Override
  public String getXULName() {
    return "/xml/hadoop.xml";
  }

  @Override
  public boolean init() throws Exception {
    status = app.find(myUi, "status");
    app.setString(status, "text", lastMsg);
    ioTable = app.find(myUi, "ioTable");
    bar = app.find(myUi, "bar");
    btOpen = app.find(myUi, "btOpen");
    indexUri = app.find(myUi, "indexUri");
    total = app.find(myUi, "totalBytes");
    bufSize = app.find(myUi, "bufSize");
    if (ir != myIr) {
      // reset ui
      lastMsg = "?";
      ioData.clear();
      app.removeAll(ioTable);
      app.setInteger(bar, "value", 0);
      totalBytes = 0;
      app.setString(total, "text", "");
    }
    app.setString(status, "text", lastMsg);
    return false;
  }
  
  public void actionClear() {
    totalBytes = 0L;
    app.setString(total, "text", String.valueOf(totalBytes));
    for (Entry<String, Row> e : ioData.entrySet()) {
      e.getValue().setCounter(0L);
    }
  }
  
  public void actionOpen() {
    final String uriTxt = app.getString(indexUri, "text");
    if (uriTxt.trim().length() == 0) {
      lastMsg = "Empty index path.";
      app.errorMsg(lastMsg);
      return;
    }
    try {
      bufferSize = Integer.parseInt(app.getString(bufSize, "text"));
    } catch (Exception e) {
      //
    }
    SlowThread st = new SlowThread(app) {
      public void execute() {
        openIndex(uriTxt);
      }
    };
    st.start();
  }
  
  private void openIndex(String uriTxt) {
    app.removeAll(ioTable);
    ioData.clear();
    actionClear();
    opening = true;
    parts = 0;
    myIr = null;
    try {
      Configuration conf = new Configuration();
      Path path = new Path(uriTxt);
      FileSystem fs = path.getFileSystem(conf);
      if (!fs.exists(path) || fs.isFile(path)) {
        lastMsg = "Path doesn't exist or is a regular file.";
        app.errorMsg(lastMsg);
        return;
      }
      IndexReader r = null;
      FileStatus[] stats = fs.listStatus(path);
      boolean hasParts = true;
      for (FileStatus s : stats) {
        if (s.isDir() && s.getPath().getName().startsWith("part-")) {
          continue;
        } else {
          hasParts = false;
          break;
        }
      }
      if (hasParts) {
        parts = stats.length;
        List<IndexReader> readers = new ArrayList<IndexReader>(stats.length);
        for (int i = 0; i < stats.length; i++) {
          app.setString(status, "text", "Opening part " + (i + 1) + " of " + stats.length + " ...");
          app.putProperty(bar, "part", new Integer(i));
          FsDirectory fsdir = 
            new FsDirectory(fs, stats[i].getPath(), false, conf,
                    new DataReporter(stats[i].getPath()), bufferSize);
          IndexReader reader = IndexReader.open(fsdir, true);
          readers.add(reader);
        }
        r = new MultiReader((IndexReader[])readers.toArray(new IndexReader[readers.size()]));
        lastMsg = "OK - sharded index (" + readers.size() + " parts)";
      } else {
        parts = 1;
        app.setString(status, "text", "Opening single index ...");
        FsDirectory fsdir = 
          new FsDirectory(fs, path, false, conf, new DataReporter(path), bufferSize);
        r = IndexReader.open(fsdir, true);
        lastMsg = "OK - single index.";
      }
      myIr = r;
      app.setSlowAccess(true);
      app.setIndexReader(r, path.toUri().toString());
      app.showStatus(lastMsg);
      app.setInteger(bar, "value", 100);
    } catch (Exception e) {
      app.errorMsg("Error: " + StringUtils.stringifyException(e));
      app.setInteger(bar, "value", 0);
      return;
    } finally {
      opening = false;
    }
  }
  
  private class Row {
    Object tableRow;
    Object counterCell;
    long counter;
    public Row(String name, long counter) {
      this.counter = counter;
      tableRow = app.create("row");
      counterCell = app.create("cell");
      app.add(tableRow, counterCell);
      app.setChoice(counterCell, "alignment", "right");
      app.setString(counterCell, "text", String.valueOf(counter));
      Object cell = app.create("cell");
      app.add(tableRow, cell);
      app.setString(cell, "text", " " + name);
    }
    
    public void incrCounter(long bytes) {
      setCounter(counter + bytes);
    }
    
    public void setCounter(long bytes) {
      counter = bytes;
      app.setString(counterCell, "text", String.valueOf(counter));
    }
  }
  
  TreeMap<String, Row> ioData = new TreeMap<String, Row>();
  
  boolean flip = false;
  boolean opening = false;
  long totalBytes = 0L;
  
  private void updateStatus(Path dir, String name, long bytes, boolean read) {
    if (opening) {
      Integer Part = (Integer)app.getProperty(bar, "part");
      if (parts > 1) {
        int delta = 100 / parts;
        int part = flip ? Part.intValue() : Part.intValue() + 1;
        int val = delta * part;
        app.setInteger(bar, "value", val);
      } else {
        if (flip) {
          app.setInteger(bar, "value", 100);
        } else {
          app.setInteger(bar, "value", 10);        
        }
      }
      flip = !flip;
    }
    String key;
    if (parts > 1) {
      key = dir.getName();
    } else {
      key = name;
    }
    if (!read) {
      key = key + " (cache)";
    }
    //System.out.println("- " + key + " " + bytes + " B");
    Row row = ioData.get(key);
    if (row == null) {
      app.removeAll(ioTable);
      row = new Row(key, bytes);
      ioData.put(key, row);
      ArrayList<String> keys = new ArrayList<String>(ioData.keySet());
      Collections.sort(keys);
      for (String k : keys) {
        app.add(ioTable, ioData.get(k).tableRow);
      }
    } else {
      row.incrCounter(bytes);
    }
    totalBytes += bytes;
    app.setString(total, "text", String.valueOf(totalBytes));
    app.showSlowStatus("Read", bytes);
  }
  
  private class DataReporter implements IOReporter {
    private Path path;
    private String pathName;
    
    public DataReporter(Path path) {
      this.path = path;
      this.pathName = path.getName();
    }

    @Override
    public void reportIO(String name, long bytes, boolean read) {
      updateStatus(this.path, name, bytes, read);
    }

    @Override
    public void reportStatus(String msg) {
      //System.out.println("- DIR " + pathName + ": " + msg);
    }
    
  }

}
