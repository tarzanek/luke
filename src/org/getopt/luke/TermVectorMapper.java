package org.getopt.luke;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;

/**
 * Utility class to make it easier to handle term vectors.
 */
public class TermVectorMapper {

  @Deprecated
  public static List<IntPair> map(Terms terms, TermsEnum reuse, boolean acceptTermsOnly, boolean convertOffsets) throws IOException {
      return map(terms, acceptTermsOnly, convertOffsets); //dummy convert
  }
    
  public static List<IntPair> map(Terms terms, boolean acceptTermsOnly, boolean convertOffsets) throws IOException {
    TermsEnum te = terms.iterator();
    PostingsEnum pe = null;
    List<IntPair> res = new ArrayList<IntPair>();
    while (te.next() != null) {
      PostingsEnum newPe = te.postings(pe, PostingsEnum.OFFSETS);
      if (newPe == null) { // no positions and no offsets - just add terms if allowed
        if (!acceptTermsOnly) {
          return null;
        }
        int freq = (int)te.totalTermFreq();
        if (freq == -1) freq = 0;
        res.add(new IntPair(freq, te.term().utf8ToString()));
        continue;
      }
      pe = newPe;
      // term vectors have only one document, number 0
      if (pe.nextDoc() == PostingsEnum.NO_MORE_DOCS) { // oops
        // treat this as no positions nor offsets
        int freq = (int)te.totalTermFreq();
        if (freq == -1) freq = 0;
        res.add(new IntPair(freq, te.term().utf8ToString()));
        continue;
      }
      IntPair ip = new IntPair(pe.freq(), te.term().utf8ToString());
      for (int i = 0; i < pe.freq(); i++) {
        int pos = pe.nextPosition();
        if (pos != -1) {
          if (ip.positions == null) {
            ip.positions = new int[pe.freq()];
          }
          ip.positions[i] = pos;
        }
        if (pe.startOffset() != -1) {
          if (ip.starts == null) {
            ip.starts = new int[pe.freq()];
            ip.ends = new int[pe.freq()];
          }
          ip.starts[i] = pe.startOffset();
          ip.ends[i] = pe.endOffset();
        }
      }
      if (convertOffsets && ip.positions == null) {
        convertOffsets(ip);
      }
      res.add(ip);
    }
    return res;
  }
  
  private static void convertOffsets(IntPair ip) {
    if (ip.starts == null || ip.ends == null) {
      return;
    }
    int[] posArr = new int[ip.starts.length];
    int curPos = 0;
    int maxDelta = 3; // allow 3 characters diff, otherwise insert a skip
    int avgTermLen = 5; // assume this is the avg. term length of missing terms
    for (int m = 0; m < ip.starts.length; m++) {
      int curStart = ip.starts[m];
      if (m > 0) {
        int prevEnd = ip.ends[m - 1];
        int prevStart = ip.starts[m - 1];
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
    ip.positions = posArr;
  }
}
