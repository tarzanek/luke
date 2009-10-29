package org.getopt.luke.decoders;

import java.text.ParseException;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.index.IndexReader;

public class DateDecoder implements Decoder {

  @Override
  public String decode(IndexReader ir, String fieldName, Object value) {
    try {
      return DateTools.stringToDate(value.toString()).toString();
    } catch (ParseException e) {
      e.printStackTrace();
      return value.toString();
    }
  }
  
  public String toString() {
    return "date";
  }

}
