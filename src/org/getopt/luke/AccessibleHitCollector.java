package org.getopt.luke;

import org.apache.lucene.search.SimpleCollector;
import org.apache.lucene.search.Scorer;

public abstract class AccessibleHitCollector extends SimpleCollector {
  protected Scorer scorer;
  protected boolean shouldScore;
  protected int docBase;  

  public abstract int getTotalHits();
  
  public abstract int getDocId(int pos);
  
  public abstract float getScore(int pos);
  
  public abstract void reset();
}
