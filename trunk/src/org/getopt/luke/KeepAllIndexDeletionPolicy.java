package org.getopt.luke;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.index.IndexDeletionPolicy;

public class KeepAllIndexDeletionPolicy implements IndexDeletionPolicy {

  public void onCommit(List commits) throws IOException {
    // do nothing - keep all points
  }

  public void onInit(List commits) throws IOException {
    // do nothing - keep all points
  }

}
