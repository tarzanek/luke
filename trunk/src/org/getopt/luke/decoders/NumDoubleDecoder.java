package org.getopt.luke.decoders;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.util.NumericUtils;

public class NumDoubleDecoder implements Decoder {

  @Override
  public String decode(IndexReader ir, String fieldName, Object value) {
    return Double.toString(NumericUtils.prefixCodedToDouble(value.toString()));
  }
  
  public String toString() {
    return "numeric-double";
  }

}
