package org.getopt.luke;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.index.IndexDeletionPolicy;

public class KeepAllIndexDeletionPolicy extends IndexDeletionPolicy {

  @Override
  public void onCommit(List commits) throws IOException {
    // do nothing - keep all points
  }

  @Override
  public void onInit(List commits) throws IOException {
    // do nothing - keep all points
  }

}
