package org.getopt.luke.decoders;

import org.getopt.luke.Util;

public class BinaryDecoder implements Decoder {

  @Override
  public String decode(String fieldName, Object value) throws Exception {
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
