package org.getopt.luke;

import java.io.IOException;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;

public class AccessibleTopHitCollector extends AccessibleHitCollector {
  private TopScoreDocCollector tdc;
  private TopDocs topDocs = null;
  private int size;
  
  public AccessibleTopHitCollector(int size, boolean outOfOrder, boolean shouldScore) {
    tdc = TopScoreDocCollector.create(size, outOfOrder);
    this.shouldScore = shouldScore;
    this.outOfOrder = outOfOrder;
    this.size = size;
  }

  @Override
  public int getDocId(int pos) {
    if (topDocs == null) {
      topDocs = tdc.topDocs();
    }
    return topDocs.scoreDocs[pos].doc;
  }

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
  public boolean acceptsDocsOutOfOrder() {
    return tdc.acceptsDocsOutOfOrder();
  }

  @Override
  public void collect(int doc) throws IOException {
    tdc.collect(doc);
  }

  @Override
  public void setNextReader(AtomicReaderContext context) throws IOException {
    this.docBase = context.docBase;
    tdc.setNextReader(context);
  }

  @Override
  public void setScorer(Scorer scorer) throws IOException {
    if (shouldScore) {
      tdc.setScorer(scorer);
    } else {
      tdc.setScorer(NoScoringScorer.INSTANCE);
    }
  }

  @Override
  public void reset() {
    tdc = TopScoreDocCollector.create(size, outOfOrder);
    topDocs = null;
  }

}
