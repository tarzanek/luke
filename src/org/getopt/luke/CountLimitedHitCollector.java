package org.getopt.luke;

import java.io.IOException;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;

public class CountLimitedHitCollector extends LimitedHitCollector {
  private final int maxSize;
  private int count;
  private int lastDoc;
  private TopScoreDocCollector tdc;
  private TopDocs topDocs = null;
  private LeafReaderContext leafReaderContext=null;
    
  public CountLimitedHitCollector(int maxSize) {
    this.maxSize = maxSize;
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

  @Override
  public LeafCollector getLeafCollector(final LeafReaderContext leafReaderContext) throws IOException {
    this.docBase = leafReaderContext.docBase;
    return new LeafCollector() {
      @Override
      public void setScorer(Scorer scorer) throws IOException {
        if (shouldScore) {
          tdc.getLeafCollector(leafReaderContext).setScorer(scorer);
        } else {
          tdc.getLeafCollector(leafReaderContext).setScorer(NoScoringScorer.INSTANCE);
        }

      }

      @Override
      public void collect(int doc) throws IOException {
        count++;
        if (count > maxSize) {
          count--;
          throw new LimitedException(TYPE_SIZE, maxSize, count, lastDoc);
        }
        lastDoc = docBase + doc;
        tdc.getLeafCollector(leafReaderContext).collect(doc);
      }
    };
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
  public void reset() {
    count = 0;
    lastDoc = 0;
    topDocs = null;
    tdc = TopScoreDocCollector.create(maxSize);
  }

    @Override
    public boolean needsScores() {
        return tdc.needsScores();
    }
}
