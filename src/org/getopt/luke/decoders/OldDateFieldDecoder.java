package org.getopt.luke.decoders;

import org.apache.lucene.document.DateField;

public class OldDateFieldDecoder implements Decoder {

  @Override
  public String decodeTerm(String fieldName, Object value) throws Exception {
    return DateField.stringToDate(value.toString()).toString();
  }
  
  @Override
  public String decodeStored(String fieldName, Object value) throws Exception {
    return decodeTerm(fieldName, value);
  }
  
  public String toString() {
    return "(old DateField)";
  }

}
