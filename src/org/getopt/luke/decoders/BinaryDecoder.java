package org.getopt.luke.decoders;

import org.apache.lucene.document.Field;
import org.apache.lucene.util.BytesRef;
import org.getopt.luke.Util;

public class BinaryDecoder implements Decoder {

  @Override
  public String decodeTerm(String fieldName, Object value) throws Exception {
    byte[] data;
    if (value instanceof BytesRef) {
      return Util.bytesToHex((BytesRef)value, false);
    } else if (value instanceof byte[]) {
      data = (byte[])value;
    } else {
      data = value.toString().getBytes();
    }
    return Util.bytesToHex(data, 0, data.length, false);
  }

  @Override
  public String decodeStored(String fieldName, Field value) throws Exception {
    if (value.binaryValue() != null) {
      return decodeTerm(fieldName, value.binaryValue());
    } else {
      return decodeTerm(fieldName, value);
    }
  }
  
  public String toString() {
    return "binary";
  }
}
