package org.getopt.luke;

import java.io.IOException;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;

public class AccessibleTopHitCollector extends AccessibleHitCollector {
  private TopScoreDocCollector tdc;  
  private LeafReaderContext leafContext = null;
  private TopDocs topDocs = null;
  private final int size;
    
  public AccessibleTopHitCollector(int size) {      
    tdc = TopScoreDocCollector.create(size);        
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
      tdc.getLeafCollector(leafContext).collect(doc);    
  }

  @Override
  public void reset() {
    tdc = TopScoreDocCollector.create(size);
    topDocs = null;
  }

    @Override
    public boolean needsScores() {
        return tdc.needsScores();
    }
    
}
