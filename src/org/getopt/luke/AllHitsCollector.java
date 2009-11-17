/**
 * 
 */
package org.getopt.luke;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Scorer;

class AllHitsCollector extends AccessibleHitCollector {
  private ArrayList<AllHit> hits = new ArrayList<AllHit>();
  
  public AllHitsCollector(boolean outOfOrder, boolean shouldScore) {
    this.outOfOrder = outOfOrder;
    this.shouldScore = shouldScore;
  }
  
  public void collect(int doc) {
    float score = 1.0f;
    if (shouldScore) {
      try {
        score = scorer.score();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    hits.add(new AllHit(docBase + doc, score));
  }
  
  public int getTotalHits() {
    return hits.size();
  }
  
  public int getDocId(int i) {
    return ((AllHitsCollector.AllHit)hits.get(i)).docId;
  }

  public float getScore(int i) {
    return ((AllHitsCollector.AllHit)hits.get(i)).score;
  }

  private static class AllHit {
    public int docId;
    public float score;
    
    public AllHit(int docId, float score) {
      this.docId = docId;
      this.score = score;
    }
  }

  @Override
  public boolean acceptsDocsOutOfOrder() {
    return outOfOrder;
  }

  @Override
  public void setNextReader(IndexReader r, int docBase) throws IOException {
    this.docBase = docBase;
  }

  @Override
  public void setScorer(Scorer scorer) throws IOException {
    this.scorer = scorer;
  }

  @Override
  public void reset() {
    hits.clear();
  }
}