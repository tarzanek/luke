package org.getopt.luke.decoders;

import org.apache.lucene.util.NumericUtils;

public class NumDoubleDecoder implements Decoder {

  @Override
  public String decodeTerm(String fieldName, Object value) {
    return Double.toString(NumericUtils.prefixCodedToDouble(value.toString()));
  }
  
  @Override
  public String decodeStored(String fieldName, Object value) {
    return value.toString();
  }
  
  public String toString() {
    return "numeric-double";
  }

}
