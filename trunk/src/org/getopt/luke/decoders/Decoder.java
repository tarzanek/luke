package org.getopt.luke.decoders;

import org.apache.lucene.index.IndexReader;

public interface Decoder {
  
  public String decode(IndexReader ir, String fieldName, Object value);
  
  public String toString();

}
