package org.getopt.luke.decoders;

import org.apache.lucene.document.DateField;

public class OldDateFieldDecoder implements Decoder {

  @Override
  public String decode(String fieldName, Object value) throws Exception {
    return DateField.stringToDate(value.toString()).toString();
  }
  
  public String toString() {
    return "(old DateField)";
  }

}
