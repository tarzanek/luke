package org.apache.lucene.index;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.lucene.index.codecs.CodecProvider;
import org.apache.lucene.index.codecs.standard.StandardCodec;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IndexInput;
import org.getopt.luke.KeepAllIndexDeletionPolicy;

/**
 * This class allows us to peek at various Lucene internals, not available
 * through public APIs (for good reasons, but inquiring minds want to know ...).
 * 
 * @author ab
 *
 */
public class IndexGate {
  static Field deletable = null;
  static Field hasChanges = null;
  static PrintStream infoStream = IndexWriter.getDefaultInfoStream();
  static HashMap<String, String> knownExtensions = new HashMap<String, String>();
  
  static {
    knownExtensions.put(IndexFileNames.COMPOUND_FILE_EXTENSION, "compound file with various index data");
    knownExtensions.put(IndexFileNames.COMPOUND_FILE_STORE_EXTENSION, "compound shared doc store file");
    knownExtensions.put(IndexFileNames.DELETABLE, "list of deletable files (pre-lockless index)");
    knownExtensions.put(IndexFileNames.DELETES_EXTENSION, "list of deleted documents");
    knownExtensions.put(IndexFileNames.FIELD_INFOS_EXTENSION, "field names / infos");
    knownExtensions.put(IndexFileNames.FIELDS_EXTENSION, "stored fields data");
    knownExtensions.put(IndexFileNames.FIELDS_INDEX_EXTENSION, "stored fields index data");
    knownExtensions.put(IndexFileNames.GEN_EXTENSION, "generation number - global file");
    knownExtensions.put(IndexFileNames.NORMS_EXTENSION, "norms data for all fields");
    knownExtensions.put(IndexFileNames.PLAIN_NORMS_EXTENSION, "per-field norms data");
    knownExtensions.put(IndexFileNames.SEGMENTS, "per-commit list of segments");
    knownExtensions.put(IndexFileNames.SEPARATE_NORMS_EXTENSION, "separate per-field norms data");
    knownExtensions.put(IndexFileNames.VECTORS_DOCUMENTS_EXTENSION, "term vectors document data");
    knownExtensions.put(IndexFileNames.VECTORS_FIELDS_EXTENSION, "term vector field data");
    knownExtensions.put(IndexFileNames.VECTORS_INDEX_EXTENSION, "term vectors index");
    CodecProvider codecs = CodecProvider.getDefault();
    HashSet<String> std = new HashSet<String>();
    StandardCodec.getStandardExtensions(std);
    for (String ext : codecs.getAllExtensions()) {
      if (knownExtensions.containsKey(ext)) {
        continue;
      } else {
        knownExtensions.put(ext, "codec-specific data" + (std.contains(ext) ? " (StandardCodec)" : ""));
      }
    }

    try {
      deletable = IndexFileDeleter.class.getDeclaredField("deletable");
      deletable.setAccessible(true);
      hasChanges = IndexReader.class.getDeclaredField("hasChanges");
      hasChanges.setAccessible(true);
    } catch (Exception e) {
      e.printStackTrace();
    }
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
  
  public static int getIndexFormat(final Directory dir) throws Exception {
    SegmentInfos.FindSegmentsFile fsf = new SegmentInfos.FindSegmentsFile(dir) {

      protected Object doBody(String segmentsFile) throws CorruptIndexException,
          IOException {
        IndexInput in = dir.openInput(segmentsFile);
        Integer indexFormat = new Integer(in.readInt());
        in.close();
        return indexFormat;
      }
    };
    Integer indexFormat = (Integer)fsf.run();
    return indexFormat.intValue();
  }
  
  public static int getCurrentIndexFormat() {
    return SegmentInfos.CURRENT_FORMAT;
  }
  
  public static FormatDetails getFormatDetails(int format) {
    FormatDetails res = new FormatDetails();
    switch (format) {
    case SegmentInfos.FORMAT:
      res.capabilities = "old plain";
      res.genericName = "Lucene Pre-2.1";
      break;
    case SegmentInfos.FORMAT_LOCKLESS:
      res.capabilities = "lock-less";
      res.genericName = "Lucene 2.1";
      break;
    case SegmentInfos.FORMAT_SINGLE_NORM_FILE:
      res.capabilities = "lock-less, single norms file";
      res.genericName = "Lucene 2.2";
      break;
    case SegmentInfos.FORMAT_SHARED_DOC_STORE:
      res.capabilities = "lock-less, single norms file, shared doc store";
      res.genericName = "Lucene 2.3";
      break;
    case SegmentInfos.FORMAT_CHECKSUM:
      res.capabilities = "lock-less, single norms, shared doc store, checksum";
      res.genericName = "Lucene 2.4";
      break;
    case SegmentInfos.FORMAT_DEL_COUNT:
      res.capabilities = "lock-less, single norms, shared doc store, checksum, del count";
      res.genericName = "Lucene 2.4";
      break;
    case SegmentInfos.FORMAT_HAS_PROX:
      res.capabilities = "lock-less, single norms, shared doc store, checksum, del count, omitTf";
      res.genericName = "Lucene 2.4";
      break;
    case SegmentInfos.FORMAT_USER_DATA:
      res.capabilities = "lock-less, single norms, shared doc store, checksum, del count, omitTf, user data";
      res.genericName = "Lucene 2.9-dev";
      break;
    case SegmentInfos.FORMAT_DIAGNOSTICS:
      res.capabilities = "lock-less, single norms, shared doc store, checksum, del count, omitTf, user data, diagnostics";
      res.genericName = "Lucene 2.9";
      break;
    case SegmentInfos.FORMAT_FLEX_POSTINGS:
      res.capabilities = "lock-less, single norms, shared doc store, checksum, del count, omitTf, user data, diagnostics, flex";
      res.genericName = "Lucene 3.1";
      break;
    default:
      res.capabilities = "unknown";
      res.genericName = "Lucene 1.3 or prior";
      break;
    }
    if (SegmentInfos.CURRENT_FORMAT > format) {
      res.capabilities = "(WARNING: newer version of Lucene that this tool)";
      res.genericName = "UNKNOWN";
    }
    return res;
  }
  
  public static boolean preferCompoundFormat(Directory dir) throws Exception {
    SegmentInfos infos = new SegmentInfos();
    infos.read(dir);
    int compound = 0, nonCompound = 0;
    for (int i = 0; i < infos.size(); i++) {
      if (((SegmentInfo)infos.get(i)).getUseCompoundFile()) {
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
    IndexFileDeleter deleter = new IndexFileDeleter(dir, policy, infos, infoStream, null, CodecProvider.getDefault());
    deleter.close();
  }
  
  public static List<String> getDeletableFiles(Directory dir) throws Exception {
    SegmentInfos infos = new SegmentInfos();
    infos.read(dir);
    IndexFileDeleter deleter = new IndexFileDeleter(dir, new KeepAllIndexDeletionPolicy(), infos, infoStream, null, CodecProvider.getDefault());
    return (List<String>)deletable.get(deleter);
  }
  
  public static List<String> getIndexFiles(Directory dir) throws Exception {
    SegmentInfos infos = new SegmentInfos();
    infos.read(dir);
    ArrayList<String> names = new ArrayList<String>();
    for (int i = 0; i < infos.size(); i++) {
      SegmentInfo info = (SegmentInfo)infos.info(i);
      names.addAll(info.files());
      names.add(info.getDelFileName());
    }
    names.add(infos.getCurrentSegmentFileName());
    names.add(IndexFileNames.SEGMENTS_GEN);
    return names;
  }
  
  public static class FormatDetails {
    public String genericName = "N/A";
    public String capabilities = "N/A";
  }
  
  public static boolean hasChanges(IndexReader ir) {
    if (ir == null) {
      return false;
    }
    try {
      return hasChanges.getBoolean(ir);
    } catch (Exception e) {
      return false;
    }
  }

}
