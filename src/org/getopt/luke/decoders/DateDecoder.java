package org.getopt.luke.decoders;

import org.apache.lucene.document.DateTools;

import org.apache.lucene.document.Field;

public class DateDecoder implements Decoder {

  @Override
  public String decodeTerm(String fieldName, Object value) throws Exception {
    return DateTools.stringToDate(value.toString()).toString();
  }
  
  @Override
  public String decodeStored(String fieldName, Field value) throws Exception {
    return decodeTerm(fieldName, value.stringValue());
  }
  
  public String toString() {
    return "date";
  }
}
