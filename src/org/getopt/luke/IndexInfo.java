package org.getopt.luke;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.FieldsEnum;
import org.apache.lucene.index.IndexGate;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.index.IndexGate.FormatDetails;
import org.apache.lucene.store.Directory;

public class IndexInfo {
  private IndexReader reader;
  private Directory dir;
  private String indexPath;
  private long totalFileSize;
  private int numTerms = -1;
  private FormatDetails formatDetails;
  private TermStats[] topTerms = null;
  private List<String> fieldNames;
  private String lastModified;
  private String version;
  private String dirImpl;
  private HashMap<String,FieldTermCount> termCounts = null;
  
  public IndexInfo(IndexReader reader, String indexPath) throws Exception {
    this.reader = reader;
    this.dir = null;
    this.dirImpl = "N/A";
    if (reader instanceof DirectoryReader) {
      this.dir = ((DirectoryReader)reader).directory();
      this.dirImpl = dir.getClass().getName();
      this.version = Long.toString(((DirectoryReader)reader).getVersion());
    }
    this.indexPath = indexPath;
    lastModified = "N/A";
    totalFileSize = dir == null ? -1 : Util.calcTotalFileSize(indexPath, dir);
    fieldNames = new ArrayList<String>();
    fieldNames.addAll(Util.fieldNames(reader, false));
    Collections.sort(fieldNames);
    if (dir != null) {
      formatDetails = IndexGate.getIndexFormat(dir);
    } else {
      formatDetails = new FormatDetails();
    }
  }

  private void countTerms() throws Exception {
    termCounts = new HashMap<String,FieldTermCount>();
    numTerms = 0;
    Fields fields = MultiFields.getFields(reader);
    FieldsEnum fe = fields.iterator();
    String fld = null;
    TermsEnum te = null;
    while ((fld = fe.next()) != null) {
      FieldTermCount ftc = new FieldTermCount();
      ftc.fieldname = fld;
      Terms terms = fe.terms();
      if (terms != null) { // count terms
        te = terms.iterator(te);
        while (te.next() != null) {
          ftc.termCount++;
          numTerms++;
        }
      }
      termCounts.put(fld, ftc);
    }
  }
  
  /**
   * @return the reader
   */
  public IndexReader getReader() {
    return reader;
  }

  public Directory getDirectory() {
    return dir;
  }
  
  /**
   * @return the indexPath
   */
  public String getIndexPath() {
    return indexPath;
  }

  /**
   * @return the totalFileSize
   */
  public long getTotalFileSize() {
    return totalFileSize;
  }

  /**
   * @return the numTerms
   */
  public int getNumTerms() throws Exception {
    if (numTerms == -1) {
      countTerms();
    }
    return numTerms;
  }

  /**
   * @return the formatDetails
   */
  public FormatDetails getIndexFormat() {
    return formatDetails;
  }
  
  public Map<String,FieldTermCount> getFieldTermCounts() throws Exception {
    if (termCounts == null) {
      countTerms();
    }
    return termCounts;
  }

  /**
   * @return the topTerms
   */
  public TermStats[] getTopTerms() throws Exception {
    if (topTerms == null) {
      topTerms = HighFreqTerms.getHighFreqTerms(reader, 50, null);
    }
    return topTerms;
  }

  /**
   * @return the fieldNames
   */
  public List<String> getFieldNames() {
    return fieldNames;
  }

  /**
   * @return the lastModified
   */
  public String getLastModified() {
    return lastModified;
  }
  
  public String getVersion() {
    return version;
  }
  
  public String getDirImpl() {
    return dirImpl;
  }

}
