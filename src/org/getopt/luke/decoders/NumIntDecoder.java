package org.getopt.luke.decoders;

import org.apache.lucene.document.Field;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.NumericUtils;

public class NumIntDecoder implements Decoder {

  @Override
  public String decodeTerm(String fieldName, Object value) {
    BytesRef ref = new BytesRef(value.toString());
    return Integer.toString(NumericUtils.prefixCodedToInt(ref));
  }
  
  @Override
  public String decodeStored(String fieldName, Field value) {
    if (value.numericValue() != null) {
      return value.numericValue().toString();
    } else {
      return value.stringValue();
    }
  }
  
  public String toString() {
    return "numeric-int";
  }

}
