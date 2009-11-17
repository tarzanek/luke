package org.getopt.luke.decoders;

import org.apache.lucene.util.NumericUtils;

public class NumLongDecoder implements Decoder {

  @Override
  public String decode(String fieldName, Object value) {
    return Long.toString(NumericUtils.prefixCodedToLong(value.toString()));
  }
  
  public String toString() {
    return "numeric-long";
  }

}
