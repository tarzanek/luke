package org.getopt.luke.decoders;

import org.apache.lucene.document.Fieldable;
import org.apache.lucene.util.NumericUtils;

public class NumLongDecoder implements Decoder {

  @Override
  public String decodeTerm(String fieldName, Object value) {
    return Long.toString(NumericUtils.prefixCodedToLong(value.toString()));
  }
  
  @Override
  public String decodeStored(String fieldName, Fieldable value) {
    return value.stringValue();
  }
  
  public String toString() {
    return "numeric-long";
  }

}
