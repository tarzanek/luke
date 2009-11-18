package org.getopt.luke.decoders;

import org.getopt.luke.Util;

public class BinaryDecoder implements Decoder {

  @Override
  public String decode(String fieldName, Object value) throws Exception {
    byte[] data;
    if (value instanceof byte[]) {
      data = (byte[])value;
    } else {
      data = value.toString().getBytes();
    }
    return Util.bytesToHex(data, 0, data.length, false);
  }
  
  public String toString() {
    return "binary";
  }
}
