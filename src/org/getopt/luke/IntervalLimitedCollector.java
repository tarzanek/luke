package org.getopt.luke;

import java.io.IOException;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TimeLimitingCollector;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.TimeLimitingCollector.TimeExceededException;

public class IntervalLimitedCollector extends LimitedHitCollector {
  private final long maxTime;
  private long lastDoc = 0;
  private TopScoreDocCollector tdc;
  private TopDocs topDocs = null;
  private TimeLimitingCollector thc;
  
  //TODO remove - outOfOrder
  public IntervalLimitedCollector(int maxTime, boolean outOfOrder, boolean shouldScore) {
    this.maxTime = maxTime;
    this.outOfOrder = outOfOrder;
    this.shouldScore = shouldScore;
    tdc = TopScoreDocCollector.create(1000);
    thc = new TimeLimitingCollector(tdc, TimeLimitingCollector.getGlobalCounter(), maxTime);
  }

  /* (non-Javadoc)
   * @see org.getopt.luke.LimitedHitCollector#limitSize()
   */
  @Override
  public long limitSize() {
    return maxTime;
  }

  /* (non-Javadoc)
   * @see org.getopt.luke.LimitedHitCollector#limitType()
   */
  @Override
  public int limitType() {
    return TYPE_TIME;
  }

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

  @Override
  public int getTotalHits() {
    return tdc.getTotalHits();
  }

  @Override
  public void collect(int docNum) throws IOException {
    try {
//TODO_L5      thc.collect(docNum);
    } catch (TimeExceededException tee) {
      // re-throw
      throw new LimitedException(TYPE_TIME, maxTime, tee.getTimeElapsed(), tee.getLastDocCollected());
    }
  }

  @Override
  public void doSetNextReader(LeafReaderContext context) throws IOException {
    this.docBase = context.docBase;
//TODO_L5    thc.setNextReader(context);
  }

  @Override
  public void setScorer(Scorer scorer) throws IOException {
    this.scorer = scorer;
    if (shouldScore) {
//TODO_L5      thc.setScorer(scorer);
    } else {
//TODO_L5      thc.setScorer(NoScoringScorer.INSTANCE);
    }
  }

  @Override
  public void reset() {
    lastDoc = 0;
    tdc = TopScoreDocCollector.create(1000);
    thc = new TimeLimitingCollector(tdc, TimeLimitingCollector.getGlobalCounter(), maxTime);
  }
}
