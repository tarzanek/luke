package org.getopt.luke.decoders;

public class StringDecoder implements Decoder {

  @Override
  public String decodeTerm(String fieldName, Object value) {
    return value != null ? value.toString() : "(null)";
  }
  
  @Override
  public String decodeStored(String fieldName, Object value) {
    return decodeTerm(fieldName, value);
  }

  public String toString() {
    return "string utf8";
  }
}
