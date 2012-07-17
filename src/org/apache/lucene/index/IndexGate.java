package org.apache.lucene.index;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.codecs.Codec;
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.util.Version;
import org.getopt.luke.Luke;
import org.getopt.luke.KeepAllIndexDeletionPolicy;

/**
 * This class allows us to peek at various Lucene internals, not available
 * through public APIs (for good reasons, but inquiring minds want to know ...).
 * 
 * @author ab
 *
 */
public class IndexGate {
  static HashMap<String, String> knownExtensions = new HashMap<String, String>();
  
  static {
    knownExtensions.put(IndexFileNames.COMPOUND_FILE_EXTENSION, "compound file with various index data");
    knownExtensions.put(IndexFileNames.COMPOUND_FILE_ENTRIES_EXTENSION, "compound file entries list");
    knownExtensions.put(IndexFileNames.GEN_EXTENSION, "generation number - global file");
    knownExtensions.put(IndexFileNames.SEGMENTS, "per-commit list of segments and user data");
  }
  
  public static String getFileFunction(String file) {
    if (file == null || file.trim().length() == 0) return file;
    String res = null;
    file = file.trim();
    int idx = file.indexOf('.');
    String suffix = null;
    if (idx != -1) {
      suffix = file.substring(idx + 1);
    }
    if (suffix == null) {
      if (file.startsWith("segments_")) {
        return knownExtensions.get(IndexFileNames.SEGMENTS);
      }
    } else {
      res = knownExtensions.get(suffix);
      if (res != null) {
        return res;
      }
      // perhaps per-field norms?
      if (suffix.length() == 2) {
        res = knownExtensions.get(suffix.substring(0, 1));
      }
    }
    return res;
  }
  
  public static FormatDetails getIndexFormat(final Directory dir) throws Exception {
    SegmentInfos.FindSegmentsFile fsf = new SegmentInfos.FindSegmentsFile(dir) {

      protected Object doBody(String segmentsFile) throws CorruptIndexException,
          IOException {
        FormatDetails res = new FormatDetails();
        res.capabilities = "unknown";
        res.genericName = "unknown";
        IndexInput in = dir.openInput(segmentsFile, IOContext.READ);
        try {
          int indexFormat = in.readInt();
          if (indexFormat == CodecUtil.CODEC_MAGIC) {
            res.genericName = "Lucene 4.x";
            res.capabilities = "flexible, codec-specific";
            int actualVersion = SegmentInfos.VERSION_40;
            try {
              actualVersion = CodecUtil.checkHeaderNoMagic(in, "segments", SegmentInfos.VERSION_40, Integer.MAX_VALUE);
              if (actualVersion > SegmentInfos.VERSION_40) {
                res.capabilities += " (WARNING: newer version of Lucene that this tool)";
              }
            } catch (Exception e) {
              e.printStackTrace();
              res.capabilities += " (error reading: " + e.getMessage() + ")";
            }
            res.genericName = "Lucene 4." + actualVersion;
          } else {
            res.genericName = "Lucene 3.x or prior";
          }
        } finally {
          in.close();          
        }
        return res;
      }
    };
    return (FormatDetails)fsf.run();
  }
  
  public static boolean preferCompoundFormat(Directory dir) throws Exception {
    SegmentInfos infos = new SegmentInfos();
    infos.read(dir);
    int compound = 0, nonCompound = 0;
    for (int i = 0; i < infos.size(); i++) {
      if (((SegmentInfoPerCommit)infos.info(i)).info.getUseCompoundFile()) {
        compound++;
      } else {
        nonCompound++;
      }
    }
    return compound > nonCompound;
  }
  
  public static void deletePendingFiles(Directory dir, IndexDeletionPolicy policy) throws Exception {
    SegmentInfos infos = new SegmentInfos();
    infos.read(dir);
    IndexWriterConfig cfg = new IndexWriterConfig(Luke.LV, new WhitespaceAnalyzer(Luke.LV));
    IndexWriter iw = new IndexWriter(dir, cfg);
    IndexFileDeleter deleter = new IndexFileDeleter(dir, policy, infos, null, iw);
    deleter.close();
    iw.close();
  }
  
  public static List<String> getDeletableFiles(Directory dir) throws Exception {
    List<String> known = getIndexFiles(dir);
    Set<String> dirFiles = new HashSet<String>(Arrays.asList(dir.listAll()));
    dirFiles.removeAll(known);
    return new ArrayList<String>(dirFiles);
   }
  
  public static List<String> getIndexFiles(Directory dir) throws Exception {
    List<IndexCommit> commits = null;
    try {
      commits = DirectoryReader.listCommits(dir);
    } catch (IndexNotFoundException e) {
      return Collections.emptyList();
    }
    Set<String> known = new HashSet<String>();
    for (IndexCommit ic : commits) {
      known.addAll(ic.getFileNames());
    }
    if (dir.fileExists(IndexFileNames.SEGMENTS_GEN)) {
      known.add(IndexFileNames.SEGMENTS_GEN);
    }
    List<String> names = new ArrayList<String>(known);
    Collections.sort(names);
    return names;
  }
  
  public static class FormatDetails {
    public String genericName = "N/A";
    public String capabilities = "N/A";
  }
}
