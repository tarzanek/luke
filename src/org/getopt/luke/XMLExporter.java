package org.getopt.luke;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.zip.GZIPOutputStream;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexReader.FieldOption;
import org.apache.lucene.search.DefaultSimilarity;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.getopt.luke.decoders.Decoder;

public class XMLExporter extends Observable {
  private IndexReader reader;
  private String indexPath;
  private boolean abort = false;
  private boolean running = false;
  private boolean decode = false;
  private ProgressNotification pn = new ProgressNotification();
  private List<String> fieldNames;
  private Map<String,Decoder> decoders;
  private DefaultSimilarity similarity = new DefaultSimilarity();
  
  public XMLExporter(IndexReader reader, String indexPath, Map<String, Decoder> decoders) {
    if (reader.getSequentialSubReaders() != null) {
      this.reader = new SlowMultiReaderWrapper(reader);
    } else {
      this.reader = reader;
    }
    this.indexPath = indexPath;
    this.decoders = decoders;
    // dump in predictable order
    fieldNames = new ArrayList<String>();
    fieldNames.addAll(reader.getFieldNames(FieldOption.ALL));
    Collections.sort(fieldNames);
  }
  
  public void abort() {
    abort = true;
  }
  
  public boolean isAborted() {
    return abort;
  }
  
  public boolean exportJS(String outputFile, boolean decode, boolean gzip, boolean preamble, boolean info,
          String rootElementName) throws Exception {
    OutputStream out;
    if (gzip) {
      out = new GZIPOutputStream(new FileOutputStream(outputFile));
    } else {
      out = new FileOutputStream(outputFile);
    }
    return export(out, decode, preamble, info, rootElementName, null);
  }
  
  /**
   * 
   * @param output output stream
   * @param decode use defined field value decoders
   * @param preamble include XML preamble
   * @param info include index info section
   * @param rootElementName name of the root XML elements
   * @param ranges if non-null then export only these ranges of documents
   * @return
   * @throws Exception
   */
  public boolean export(OutputStream output, boolean decode, boolean preamble, boolean info,
      String rootElementName, Ranges ranges) throws Exception {
    running = true;
    pn.message = "Export running ...";
    pn.minValue = 0;
    pn.maxValue = reader.maxDoc();
    pn.curValue = 0;
    setChanged();
    notifyObservers(pn);
    if (rootElementName == null) {
      rootElementName = "index";
    }
    if (decoders == null || decoders.isEmpty()) {
      decode = false;
    }
    BufferedWriter bw;
    boolean rootWritten = false;
    int delta = reader.maxDoc() / 100;
    if (delta == 0) delta = 1;
    int cnt = 0;
    bw = new BufferedWriter(new OutputStreamWriter(output, "UTF-8"));
    Bits live = reader.getLiveDocs();
    try {
      // write out XML preamble
      if (preamble) {
        bw.write("<?xml version='1.0' encoding='UTF-8'?>\n");
      }
      bw.write("<" + rootElementName + ">\n");
      rootWritten = true;
      if (info) {
        // write out some statistics
        writeIndexInfo(bw);
      }
      Document doc = null;
      int i = -1;
      if (ranges == null) {
        ranges = new Ranges();
        ranges.set(0, reader.maxDoc());
      }
      if (ranges.cardinality() > 0) {
        while ( (i = ranges.nextSetBit(++i)) != -1) {
          if (i >= reader.maxDoc()) {
            break;
          }
          if (abort) {
            pn.message = "User requested abort.";
            pn.aborted = true;
            running = false;
            setChanged();
            notifyObservers(pn);
            break;
          }
          if (live != null && !live.get(i)) continue; // skip deleted docs
          doc = reader.document(i);
          // write out fields
          writeDoc(bw, i, doc, decode);
          pn.curValue = i + 1;
          cnt++;
          if (cnt > delta) {
            cnt = 0;
            setChanged();
            notifyObservers(pn);
          }
        }
      }
    } catch (Exception ioe) {
      ioe.printStackTrace();
      pn.message = "ERROR creating output: " + ioe.toString();
      pn.aborted = true;
      running = false;
      setChanged();
      notifyObservers(pn);
      return false;
    } finally {
      if (bw != null) {
        try {
          if (rootWritten) { // balance the top tag
            bw.write("</" + rootElementName + ">");
          }
          bw.flush();
        } catch (Exception e) {
          pn.message = "ERROR closing output: " + e.toString();
          pn.aborted = true;
          running = false;
          setChanged();
          notifyObservers(pn);
          return false;
        }
      }
    }
    pn.message = "Finished.";
    setChanged();
    notifyObservers(pn);
    running = false;
    return !pn.aborted;
  }
  
  private void writeDoc(BufferedWriter bw, int docNum, Document doc, boolean decode) throws Exception {
    bw.write("<doc id='" + docNum + "'>\n");
    for (String fieldName : fieldNames) {
      Field[] fields = doc.getFields(fieldName);
      if (fields == null || fields.length == 0) {
        continue;
      }
      bw.write("<field name='" + Util.xmlEscape(fields[0].name()));
      if (reader.hasNorms(fields[0].name())) {
        bw.write("' norm='" + similarity.decodeNormValue(reader.norms(fields[0].name())[docNum]));
      } 
      bw.write("' flags='" + Util.fieldFlags(fields[0]) + "'>\n");
      for (Field f : fields) {
        String val = null;
        if (decode) {
          Decoder d = decoders.get(f.name());
          if (d != null) {
            val = d.decodeStored(f.name(), f);
          }
        }
        if (!decode || val == null) {
          if (f.isBinary()) {
            val = Util.bytesToHex(f.getBinaryValue(),
                    f.getBinaryOffset(), f.getBinaryLength(), false);
          } else {
            val = f.stringValue();
          }
        }
        bw.write("<val>" + Util.xmlEscape(val) + "</val>\n");
      }
      TermFreqVector tfv = reader.getTermFreqVector(docNum, fieldName);
      if (tfv != null) {
        writeTermVector(bw, tfv);
      }
      bw.write("</field>\n");
    }
    bw.write("</doc>\n");
  }
  
  private void writeTermVector(BufferedWriter bw, TermFreqVector tfv) throws Exception {
    bw.write("<tv size='" + tfv.size() + "'>\n");
    BytesRef[] terms = tfv.getTerms();
    int[] freqs = tfv.getTermFrequencies();
    for (int k = 0; k < terms.length; k++) {
      int[] posArray = null;
      TermVectorOffsetInfo[] offsets = null;
      if (tfv instanceof TermPositionVector) {
        posArray = ((TermPositionVector)tfv).getTermPositions(k);
        offsets = ((TermPositionVector)tfv).getOffsets(k);
      }
      StringBuilder sb = new StringBuilder();
      if (posArray != null) {
        sb.append(" positions='");
        for (int i = 0; i < posArray.length; i++) {
          if (i > 0) sb.append(' ');
          sb.append(String.valueOf(posArray[i]));
        }
        sb.append("'");
      }
      if (offsets != null) {
        sb.append(" offsets='");
        for (int i = 0; i < offsets.length; i++) {
          if (i > 0) sb.append(' ');
          sb.append(offsets[i].getStartOffset() + "-" + offsets[i].getEndOffset());
        }
        sb.append("'");
      }
      bw.write("<t text='" + Util.xmlEscape(terms[k].utf8ToString()) + "' freq='" + freqs[k] + "'" + sb.toString() + "/>\n");
    }
    bw.write("</tv>\n");
  }
  
  private void writeIndexInfo(BufferedWriter bw) throws Exception {
    bw.write("<info>\n");
    IndexInfo indexInfo = new IndexInfo(reader, indexPath);
    bw.write(" <indexPath>" + Util.xmlEscape(indexPath) + "</indexPath>\n");
    bw.write(" <fields count='" + indexInfo.getFieldNames().size() + "'>\n");
    for (String fname : indexInfo.getFieldNames()) {
      bw.write("  <field name='" + Util.xmlEscape(fname) + "'/>\n");
    }
    bw.write(" </fields>\n");
    bw.write(" <numDocs>" + reader.numDocs() + "</numDocs>\n");
    bw.write(" <maxDoc>" + reader.maxDoc() + "</maxDoc>\n");
    bw.write(" <numDeletedDocs>" + reader.numDeletedDocs() + "</numDeletedDocs>\n");
    bw.write(" <numTerms>" + indexInfo.getNumTerms() + "</numTerms>\n");
    bw.write(" <hasDeletions>" + reader.hasDeletions() + "</hasDeletions>\n");
    bw.write(" <optimized>" + reader.isOptimized() + "</optimized>\n");
    bw.write(" <lastModified>" + indexInfo.getLastModified() + "</lastModified>\n");
    bw.write(" <indexVersion>" + indexInfo.getVersion() + "</indexVersion>\n");
    bw.write(" <indexFormat>\n");
    bw.write("  <id>" + indexInfo.getIndexFormat() + "</id>\n");
    bw.write("  <genericName>" + indexInfo.getFormatDetails().genericName + "</genericName>\n");
    bw.write("  <capabilities>" + indexInfo.getFormatDetails().capabilities + "</capabilities>\n");
    bw.write(" </indexFormat>\n");
    bw.write(" <directoryImpl>" + indexInfo.getDirImpl() + "</directoryImpl>\n");
    Directory dir = indexInfo.getDirectory();
    if (dir != null) {
      bw.write(" <files count='" + dir.listAll().length + "'>\n");
      String[] files = dir.listAll();
      Arrays.sort(files);
      for (String file : files) {
        bw.write("  <file name='" + file +
            "' size='" + dir.fileLength(file) +
            "' func='" + IndexGate.getFileFunction(file) + "'/>\n");
      }
      bw.write(" </files>\n");
      bw.write(" <commits count='" + IndexReader.listCommits(dir).size() + "'>\n");
      for (Object o : IndexReader.listCommits(dir)) {
        IndexCommit ic = (IndexCommit)o;
        bw.write("  <commit tstamp='" + new Date(ic.getTimestamp()).toString() +
            "' segment='" + ic.getSegmentsFileName() + "' optimized='" + ic.isOptimized() + 
            "' deleted='" + ic.isDeleted() + "' files='" + ic.getFileNames().size() + "'>\n");
        for (Object p : ic.getFileNames()) {
          bw.write("   <file name='" + p.toString() + "'/>\n");
        }
        Map<String,String> userData = ic.getUserData();
        if (userData != null && userData.size() > 0) {
          bw.write("   <userData size='" + userData.size() + "'>" + userData.toString() + "</userData>\n");
        }
        bw.write("  </commit>\n");
      }
      bw.write(" </commits>\n");
    }
    TermStats[] topTerms = indexInfo.getTopTerms();
    if (topTerms != null) {
      bw.write(" <topTerms count='" + topTerms.length + "'>\n");
      for (TermStats ts : topTerms) {
        String val = null;
        if (decode) {
          Decoder d = decoders.get(ts.field);
          if (d != null) {
            val = d.decodeTerm(ts.field, ts.termtext);
          }
        }
        if (!decode || val == null) {
          val = ts.termtext.utf8ToString();
        }
        val = Util.xmlEscape(val);
        bw.write("  <term field='" + Util.xmlEscape(ts.field) + "' text='" +
                val +
          "' docFreq='" + ts.docFreq + "'/>\n");
      }
    }
    bw.write(" </topTerms>\n");
    bw.write("</info>\n");    
  }

  /**
   * @return the running
   */
  public boolean isRunning() {
    return running;
  }
  
  public static void main(String[] args) throws Exception {
    if (args.length < 2) {
      System.err.println("Usage: XMLExporter <indexPath> <outputFile> [-gzip] [-onlyInfo] [-range ..expr..]");
      System.err.println("\tindexPath\tname of the directory containing the index");
      System.err.println("\toutputFile\toutput file, or '-' for System.out");
      System.err.println("\tgzip\tcompress output using gzip compression");
      System.err.println("\tonlyInfo\texport only the overall information about the index");
      System.err.println("\trange\tspecify ranges of documents to export. Expressions cannot contain whitespace!");
      System.err.println("\t\tExample: 0-5,15,32-100,101,103,105-500");
      System.exit(-1);
    }
    Directory dir = FSDirectory.open(new File(args[0]));
    if (!IndexReader.indexExists(dir)) {
      throw new Exception("There is no valid Lucene index here: '" + args[0] + "'");
    }
    File out = null;
    if (!args[1].equals("-")) {
      out = new File(args[1]);
    }
    if (out != null && out.exists()) {
      throw new Exception("Output file already exists: '" + out.getAbsolutePath() + "'");
    }
    boolean gzip = false;
    Ranges ranges = null;
    boolean onlyInfo = false;
    for (int i = 2; i < args.length; i++) {
      if (args[i].equals("-gzip")) {
        gzip = true;
      } else if (args[i].equals("-range")) {
        ranges = Ranges.parse(args[++i]);
      } else if (args[i].equals("-onlyInfo")) {
        onlyInfo = true;
      } else {
        throw new Exception("Unknown argument: '" + args[i] + "'");
      }
    }
    IndexReader reader = IndexReader.open(dir);
    XMLExporter exporter = new XMLExporter(reader, args[0], null);
    OutputStream os;
    if (out == null) {
      os = System.out;
    } else {
      os = new FileOutputStream(out);
    }
    if (gzip) {
      os = new GZIPOutputStream(os);
    }
    if (onlyInfo) {
      ranges = new Ranges();
    }
    exporter.export(os, false, false, true, "index", ranges);
    os.flush();
    os.close();
    System.exit(0);
  }
}
