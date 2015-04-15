package org.getopt.luke;

import java.io.IOException;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;

public class CountLimitedHitCollector extends LimitedHitCollector {
  private final int maxSize;
  private int count;
  private int lastDoc;
  private TopScoreDocCollector tdc;
  private TopDocs topDocs = null;
  
  //TODO remove outOfOrder
  public CountLimitedHitCollector(int maxSize, boolean outOfOrder, boolean shouldScore) {
    this.maxSize = maxSize;
    this.outOfOrder = outOfOrder;
    this.shouldScore = shouldScore;
    count = 0;
    tdc = TopScoreDocCollector.create(maxSize);
  }

  @Override
  public long limitSize() {
    return maxSize;
  }

  @Override
  public int limitType() {
    return TYPE_SIZE;
  }

  /* (non-Javadoc)
   * @see org.getopt.luke.AllHitsCollector#collect(int, float)
   */
  @Override
  public void collect(int doc) throws IOException {
    count++;
    if (count > maxSize) {
      count--;
      throw new LimitedException(TYPE_SIZE, maxSize, count, lastDoc);
    }
    lastDoc = docBase + doc;
    
//TODO_L5    tdc.collect(doc);
  }

  /* (non-Javadoc)
   * @see org.getopt.luke.AccessibleHitCollector#getDocId(int)
   */
  @Override
  public int getDocId(int pos) {
    if (topDocs == null) {
      topDocs = tdc.topDocs();
    }
    return topDocs.scoreDocs[pos].doc;
  }

  /* (non-Javadoc)
   * @see org.getopt.luke.AccessibleHitCollector#getScore(int)
   */
  @Override
  public float getScore(int pos) {
    if (topDocs == null) {
      topDocs = tdc.topDocs();
    }
    return topDocs.scoreDocs[pos].score;
  }

  /* (non-Javadoc)
   * @see org.getopt.luke.AccessibleHitCollector#getTotalHits()
   */
  @Override
  public int getTotalHits() {
    return count;
  }

  @Override
  public void doSetNextReader(LeafReaderContext context) throws IOException {
    this.docBase = context.docBase;
//TODO_L5    tdc.SetNextReader(context);
  }

  @Override
  public void setScorer(Scorer scorer) throws IOException {
    if (shouldScore) {
      //TODO_L5 tdc.setScorer(scorer);
    } else {
      //TODO_L5 tdc.setScorer(NoScoringScorer.INSTANCE);
    }
  }

  @Override
  public void reset() {
    count = 0;
    lastDoc = 0;
    topDocs = null;
    tdc = TopScoreDocCollector.create(maxSize);
  }

    @Override
    public boolean needsScores() {
        return shouldScore;
    }
}
