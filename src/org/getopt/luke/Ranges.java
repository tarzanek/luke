package org.getopt.luke;

import org.apache.lucene.util.FixedBitSet;        

@SuppressWarnings("serial")
public class Ranges {
  FixedBitSet data=null;
  public static Ranges parse(String expr) throws Exception {
    Ranges res=new Ranges();    
    expr = expr.replaceAll("\\s+", "");
    if (expr.length() == 0) {
      return res;
    }
    String[] ranges = expr.split(",");
      for (String range : ranges) {
          String[] ft = range.split("-");
          int from, to;
          from = Integer.parseInt(ft[0]);
          if (ft.length == 1) {
              res.data.set(from);
          } else {
              to = Integer.parseInt(ft[1]);
              res.data.set(from, to);
          }
      }
    return res;
  }
  
  public Ranges() {
     data = new FixedBitSet(64);
  }
      
  public void set(int from, int to) {
    if (from > to) return;
    for (int i = from; i <= to; i++) {
      if (data!=null) {
         data.set(i);
      }   
    }
  }
}
