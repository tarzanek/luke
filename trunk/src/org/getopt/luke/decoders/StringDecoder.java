package org.getopt.luke.decoders;

public class StringDecoder implements Decoder {

  @Override
  public String decode(String fieldName, Object value) {
    return value != null ? value.toString() : "(null)";
  }

  public String toString() {
    return "string utf8";
  }
}
