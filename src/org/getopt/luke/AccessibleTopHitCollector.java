package org.getopt.luke;

import java.io.IOException;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;

public class AccessibleTopHitCollector extends AccessibleHitCollector {
  private TopScoreDocCollector tdc;
  private LeafCollector leafCollector = null;
  private LeafReaderContext leafContext = null;
  private TopDocs topDocs = null;
  private int size;
  
  //TODO remove outOfOrder
  public AccessibleTopHitCollector(int size, boolean outOfOrder, boolean shouldScore) {      
    tdc = TopScoreDocCollector.create(size);    
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
  public void collect(int doc) throws IOException {      
    leafCollector.collect(doc);
  }

  @Override
  public void doSetNextReader(LeafReaderContext context) throws IOException {
    this.docBase = context.docBase;
    leafContext = context;
    leafCollector = tdc.getLeafCollector(context);
    
//TODO_L5    tdc.setNextReader(context);
  }

  @Override
  public void setScorer(Scorer scorer) throws IOException {      
    if (shouldScore) {
//TODO_L5      tdc.scorer=scorer;
    } else {
//TODO_L5      tdc.scorer=NoScoringScorer.INSTANCE;
    }
  }

  @Override
  public void reset() {
    tdc = TopScoreDocCollector.create(size);
    topDocs = null;
  }

}
