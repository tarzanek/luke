package org.getopt.luke;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.Bits;

/**
 * Utility class to make it easier to handle term vectors.
 */
public class TermVectorMapper {

  public static List<IntPair> map(Terms terms, TermsEnum reuse, boolean acceptTermsOnly, boolean convertOffsets) throws IOException {
    TermsEnum te = terms.iterator(reuse);
    DocsAndPositionsEnum dpe = null;
    List<IntPair> res = new ArrayList<IntPair>();
    while (te.next() != null) {
      DocsAndPositionsEnum newDpe = te.docsAndPositions(null, dpe, DocsAndPositionsEnum.FLAG_OFFSETS);
      if (newDpe == null) { // no positions and no offsets - just add terms if allowed
        if (!acceptTermsOnly) {
          return null;
        }
        int freq = (int)te.totalTermFreq();
        if (freq == -1) freq = 0;
        res.add(new IntPair(freq, te.term().utf8ToString()));
        continue;
      }
      dpe = newDpe;
      // term vectors have only one document, number 0
      if (dpe.nextDoc() == DocsEnum.NO_MORE_DOCS) { // oops
        // treat this as no positions nor offsets
        int freq = (int)te.totalTermFreq();
        if (freq == -1) freq = 0;
        res.add(new IntPair(freq, te.term().utf8ToString()));
        continue;
      }
      IntPair ip = new IntPair(dpe.freq(), te.term().utf8ToString());
      for (int i = 0; i < dpe.freq(); i++) {
        int pos = dpe.nextPosition();
        if (pos != -1) {
          if (ip.positions == null) {
            ip.positions = new int[dpe.freq()];
          }
          ip.positions[i] = pos;
        }
        if (dpe.startOffset() != -1) {
          if (ip.starts == null) {
            ip.starts = new int[dpe.freq()];
            ip.ends = new int[dpe.freq()];
          }
          ip.starts[i] = dpe.startOffset();
          ip.ends[i] = dpe.endOffset();
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
