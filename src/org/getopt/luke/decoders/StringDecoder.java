package org.getopt.luke.decoders;

import org.apache.lucene.document.Fieldable;

public class StringDecoder implements Decoder {

  @Override
  public String decodeTerm(String fieldName, Object value) {
    return value != null ? value.toString() : "(null)";
  }
  
  @Override
  public String decodeStored(String fieldName, Fieldable value) {
    return decodeTerm(fieldName, value.stringValue());
  }

  public String toString() {
    return "string utf8";
  }
}
