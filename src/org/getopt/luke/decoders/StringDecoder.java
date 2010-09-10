package org.getopt.luke.decoders;

import org.apache.lucene.document.Fieldable;
import org.apache.lucene.util.BytesRef;

public class StringDecoder implements Decoder {
  BinaryDecoder b = new BinaryDecoder();

  @Override
  public String decodeTerm(String fieldName, Object value) {
    if (value == null) {
      return "(null)";
    } else if (value instanceof BytesRef) {
      return ((BytesRef)value).utf8ToString();
    } else {
      return value.toString();
    }
  }
  
  @Override
  public String decodeStored(String fieldName, Fieldable value) throws Exception {
    if (value.isBinary()) {
      return b.decodeStored(fieldName, value);
    }
    return decodeTerm(fieldName, value.stringValue());
  }

  public String toString() {
    return "string utf8";
  }
}
