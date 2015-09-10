package org.getopt.luke;

import java.io.IOException;

import org.apache.lucene.index.LeafReaderContext;
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
  private LeafReaderContext leafReaderContext;
    
  public IntervalLimitedCollector(int maxTime) {
    this.maxTime = maxTime;
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
       thc.getLeafCollector(leafReaderContext).collect(docNum);
    } catch (TimeExceededException tee) {
      // re-throw
      throw new LimitedException(TYPE_TIME, maxTime, tee.getTimeElapsed(), tee.getLastDocCollected());
    }
  }

  @Override
  public void reset() {
    lastDoc = 0;
    tdc = TopScoreDocCollector.create(1000);
    thc = new TimeLimitingCollector(tdc, TimeLimitingCollector.getGlobalCounter(), maxTime);
  }

    @Override
    public boolean needsScores() {
        return thc.needsScores();
    }
}
