package org.getopt.luke.decoders;

import org.apache.lucene.index.IndexReader;

public class StringDecoder implements Decoder {

  @Override
  public String decode(IndexReader ir, String fieldName, Object value) {
    return value != null ? value.toString() : "(null)";
  }

  public String toString() {
    return "string utf8";
  }
}
