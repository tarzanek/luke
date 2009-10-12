package org.getopt.luke;

import java.io.IOException;

import org.apache.lucene.search.Scorer;

public class NoScoringScorer extends Scorer {
  public static final NoScoringScorer INSTANCE = new NoScoringScorer();

  protected NoScoringScorer() {
    super(null);
  }

  @Override
  public float score() throws IOException {
    // TODO Auto-generated method stub
    return 1.0f;
  }

}
