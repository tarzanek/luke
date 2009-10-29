package org.getopt.luke.decoders;

import org.apache.lucene.index.IndexReader;
import org.getopt.luke.Util;

public class BinaryDecoder implements Decoder {

  @Override
  public String decode(IndexReader ir, String fieldName, Object value) {
    if (value instanceof byte[]) {
      byte[] data = (byte[])value;
      return Util.bytesToHex(data, 0, data.length, false);
    } else {
      return value.toString();
    }
  }
  
  public String toString() {
    return "binary";
  }
}
