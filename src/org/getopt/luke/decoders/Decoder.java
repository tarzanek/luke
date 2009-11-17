package org.getopt.luke.decoders;

public interface Decoder {
  
  public String decode(String fieldName, Object value) throws Exception;
}
