package org.getopt.luke;

public class FieldTermCount implements Comparable<FieldTermCount> {
  public String fieldname;
  public long termCount;

  public int compareTo(FieldTermCount f2) {
    if (termCount > f2.termCount) {
      return -1;
    } else if (termCount < f2.termCount) {
        return 1;
    } else {
      return 0;
    }
  }
}
