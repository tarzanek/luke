package org.getopt.luke.decoders;

import org.apache.lucene.document.NumberTools;

public class OldNumberToolsDecoder implements Decoder {

  @Override
  public String decode(String fieldName, Object value) throws Exception {
    return Long.toString(NumberTools.stringToLong(value.toString()));
  }

}
