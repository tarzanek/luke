package org.getopt.luke.decoders;

import org.apache.lucene.document.Fieldable;

public interface Decoder {
  
  public String decodeTerm(String fieldName, Object value) throws Exception;
  public String decodeStored(String fieldName, Fieldable value) throws Exception;
}
