package org.getopt.luke.decoders;

import org.apache.lucene.document.DateTools;

public class DateDecoder implements Decoder {

  @Override
  public String decode(String fieldName, Object value) throws Exception {
    return DateTools.stringToDate(value.toString()).toString();
  }
  
  public String toString() {
    return "date";
  }

}
