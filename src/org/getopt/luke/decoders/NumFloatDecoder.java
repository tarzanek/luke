package org.getopt.luke.decoders;

import org.apache.lucene.document.Fieldable;
import org.apache.lucene.util.NumericUtils;

public class NumFloatDecoder implements Decoder {

  @Override
  public String decodeTerm(String fieldName, Object value) {
    return Float.toString(NumericUtils.prefixCodedToFloat(value.toString()));
  }
  
  @Override
  public String decodeStored(String fieldName, Fieldable value) {
    return value.stringValue();
  }
  
  public String toString() {
    return "numeric-float";
  }

}
