package org.getopt.luke;

import java.util.*;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexReader.FieldOption;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;

/**
 * This class attempts to reconstruct all fields from a document
 * existing in a Lucene index. This operation may be (and usually) is
 * lossy - e.g. unstored fields are rebuilt from terms present in the
 * index, and these terms may have been changed (e.g. lowercased, stemmed),
 * and many other input tokens may have been skipped altogether by the
 * Analyzer, when fields were originally added to the index.
 * 
 * @author ab
 *
 */
public class DocReconstructor extends Observable {
  private ProgressNotification progress = new ProgressNotification();
  private String[] fieldNames = null;
  private IndexReader reader = null;
  private int numTerms;
  private Bits deleted;
  
  /**
   * Prepare a document reconstructor.
   * @param reader IndexReader to read from.
   * @throws Exception
   */
  public DocReconstructor(IndexReader reader) throws Exception {
    this(reader, null, -1);
  }
  
  /**
   * Prepare a document reconstructor.
   * @param reader IndexReader to read from.
   * @param fieldNames if non-null or not empty, data will be collected only from
   * these fields, otherwise data will be collected from all fields
   * @param numTerms total number of terms in the index, or -1 if unknown (will
   * be calculated)
   * @throws Exception
   */
  public DocReconstructor(IndexReader reader, String[] fieldNames, int numTerms) throws Exception {
    if (reader == null) {
      throw new Exception("IndexReader cannot be null.");
    }
    this.reader = reader;
    if (fieldNames == null || fieldNames.length == 0) {
      // collect fieldNames
      this.fieldNames = (String[])reader.getFieldNames(FieldOption.ALL).toArray(new String[0]);
    } else {
      this.fieldNames = fieldNames;
    }
    if (numTerms == -1) {
      Fields fields = MultiFields.getFields(reader);
      numTerms = 0;
      FieldsEnum fe = fields.iterator();
      String fld = null;
      while ((fld = fe.next()) != null) {
        TermsEnum te = fe.terms();
        while (te.next() != null) {
          numTerms++;
        }
      }
      this.numTerms = numTerms;
    }
    deleted = MultiFields.getDeletedDocs(reader);
  }
  
  /**
   * Reconstruct document fields.
   * @param docNum document number. If this document is deleted, but the index
   * is not optimized yet, the reconstruction process may still yield the
   * reconstructed field content even from deleted documents.
   * @return reconstructed document
   * @throws Exception
   */
  public Reconstructed reconstruct(int docNum) throws Exception {
    if (docNum < 0 || docNum > reader.maxDoc()) {
      throw new Exception("Document number outside of valid range.");
    }
    Reconstructed res = new Reconstructed();
    if (deleted.get(docNum)) {
      throw new Exception("Document is deleted.");
    } else {
      Document doc = reader.document(docNum);
      for (int i = 0; i < fieldNames.length; i++) {
        Field[] fs = doc.getFields(fieldNames[i]);
        if (fs != null && fs.length > 0) {
          res.getStoredFields().put(fieldNames[i], fs);
        }
      }
    }
    // collect values from unstored fields
    HashSet<String> fields = new HashSet<String>(Arrays.asList(fieldNames));
    // try to use term vectors if available
    progress.maxValue = fieldNames.length;
    progress.curValue = 0;
    progress.minValue = 0;
    for (int i = 0; i < fieldNames.length; i++) {
      TermFreqVector tvf = reader.getTermFreqVector(docNum, fieldNames[i]);
      if (tvf != null && tvf.size() > 0 && (tvf instanceof TermPositionVector)) {
        TermPositionVector tpv = (TermPositionVector)tvf;
        progress.message = "Reading term vectors ...";
        progress.curValue = i;
        setChanged();
        notifyObservers(progress);
        BytesRef[] tv = tpv.getTerms();
        for (int k = 0; k < tv.length; k++) {
          // do we have positions?
          int[] posArr = tpv.getTermPositions(k);
          if (posArr == null) {
            // only offsets
            TermVectorOffsetInfo[] offsets = tpv.getOffsets(k);
            if (offsets.length == 0) {
              continue;
            }
            // convert offsets into positions
            posArr = convertOffsets(offsets);
          }
          GrowableStringArray gsa = res.getReconstructedFields().get(fieldNames[i]);
          if (gsa == null) {
            gsa = new GrowableStringArray();
            res.getReconstructedFields().put(fieldNames[i], gsa);
          }
          for (int m = 0; m < posArr.length; m++) {
            gsa.append(posArr[m], "|", tv[k].utf8ToString());
          }
        }
        fields.remove(fieldNames[i]); // got what we wanted
      }
    }
    // this loop collects data only from left-over fields
    // not yet collected through term vectors
    progress.maxValue = fields.size();
    progress.curValue = 0;
    progress.minValue = 0;
    for (String fld : fields) {
      progress.message = "Collecting terms in " + fld + " ...";
      progress.curValue++;
      setChanged();
      notifyObservers(progress);
      Terms terms = MultiFields.getTerms(reader, fld);
      TermsEnum te = terms.iterator();
      while (te.next() != null) {
        DocsAndPositionsEnum dpe = te.docsAndPositions(deleted, null);
        if (dpe == null) { // no position info for this field
          break;
        }
        int num = dpe.advance(docNum);
        if (num != docNum) { // either greater than or NO_MORE_DOCS
          continue; // no data for this term in this doc
        }
        String term = te.term().utf8ToString();
        GrowableStringArray gsa = (GrowableStringArray)
              res.getReconstructedFields().get(fld);
        if (gsa == null) {
          gsa = new GrowableStringArray();
          res.getReconstructedFields().put(fld, gsa);
        }
        for (int k = 0; k < dpe.freq(); k++) {
          int pos = dpe.nextPosition();
          gsa.append(pos, "|", term);
        }
      }
    }
    progress.message = "Done.";
    progress.curValue = 100;
    setChanged();
    notifyObservers(progress);
    return res;
  }
  
  private int[] convertOffsets(TermVectorOffsetInfo[] offsets) {
    int[] posArr = new int[offsets.length];
    int curPos = 0;
    int maxDelta = 3; // allow 3 characters diff, otherwise insert a skip
    int avgTermLen = 5; // assume this is the avg. term length of missing terms
    for (int m = 0; m < offsets.length; m++) {
      int curStart = offsets[m].getStartOffset();
      if (m > 0) {
        int prevEnd = offsets[m - 1].getEndOffset();
        int prevStart = offsets[m - 1].getStartOffset();
        if (curStart == prevStart) {
          curPos--; // overlapping token
        } else {
          if (prevEnd + maxDelta < curStart) { // possibly a gap
            // calculate the number of missing tokens
            int increment = (curStart - prevEnd) / (maxDelta + avgTermLen);
            if (increment == 0) increment++;
            curPos += increment;
          }
        }
      }
      posArr[m] = curPos;
      curPos++;
    }
    return posArr;
  }

  /**
   * This class represents a reconstructed document.
   * @author ab
   */
  public static class Reconstructed {
    private Map<String, Field[]> storedFields;
    private Map<String, GrowableStringArray> reconstructedFields;

    public Reconstructed() {
      storedFields = new HashMap<String, Field[]>();
      reconstructedFields = new HashMap<String, GrowableStringArray>();
    }
    
    /**
     * Construct an instance of this class using existing field data.
     * @param storedFields field data of stored fields
     * @param reconstructedFields field data of unstored fields
     */
    public Reconstructed(Map<String, Field[]> storedFields,
        Map<String, GrowableStringArray> reconstructedFields) {
      this.storedFields = storedFields;
      this.reconstructedFields = reconstructedFields;
    }
    
    /**
     * Get an alphabetically sorted list of field names.
     */
    public List<String> getFieldNames() {
      HashSet<String> names = new HashSet<String>();
      names.addAll(storedFields.keySet());
      names.addAll(reconstructedFields.keySet());
      ArrayList<String> res = new ArrayList<String>(names.size());
      res.addAll(names);
      Collections.sort(res);
      return res;
    }
    
    public boolean hasField(String name) {
      return storedFields.containsKey(name) || reconstructedFields.containsKey(name);
    }

    /**
     * @return the storedFields
     */
    public Map<String, Field[]> getStoredFields() {
      return storedFields;
    }

    /**
     * @return the reconstructedFields
     */
    public Map<String, GrowableStringArray> getReconstructedFields() {
      return reconstructedFields;
    }

  }
}
