package org.getopt.luke.decoders;

import org.apache.lucene.document.Fieldable;
import org.apache.lucene.util.NumericUtils;

public class NumIntDecoder implements Decoder {

  @Override
  public String decodeTerm(String fieldName, Object value) {
    return Integer.toString(NumericUtils.prefixCodedToInt(value.toString()));
  }
  
  @Override
  public String decodeStored(String fieldName, Fieldable value) {
    return value.stringValue();
  }
  
  public String toString() {
    return "numeric-int";
  }

}
