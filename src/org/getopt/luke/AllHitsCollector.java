/**
 * 
 */
package org.getopt.luke;

import java.io.IOException;
import java.util.ArrayList;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.Scorer;

class AllHitsCollector extends AccessibleHitCollector {
  private ArrayList<AllHit> hits = new ArrayList<AllHit>();
  
  public AllHitsCollector() {
  }
   
  @Override
  public LeafCollector getLeafCollector(LeafReaderContext leafReaderContext) throws IOException {
    this.docBase = leafReaderContext.docBase;
    return new LeafCollector() {
      private Scorer scorer;

      @Override
      public void setScorer(Scorer scorer) throws IOException {
        this.scorer = scorer;
      }

      @Override
      public void collect(int doc) throws IOException {
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
    };
  }
  
  @Override
  public int getTotalHits() {
    return hits.size();
  }
  
  @Override
  public int getDocId(int i) {
    return ((AllHitsCollector.AllHit)hits.get(i)).docId;
  }

  @Override
  public float getScore(int i) {
    return ((AllHitsCollector.AllHit)hits.get(i)).score;
  }

  @Override
  public boolean needsScores() {
     return shouldScore;
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
  public void reset() {
    hits.clear();
  }
}