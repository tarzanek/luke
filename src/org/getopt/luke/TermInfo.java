package org.getopt.luke;

import org.apache.lucene.index.Term;

public class TermInfo {
  public Term term;
  public int docFreq;
  
  public TermInfo(Term t, int df) {
    this.term = t;
    this.docFreq = df;
  }

}
