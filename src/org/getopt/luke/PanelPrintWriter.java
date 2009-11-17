package org.getopt.luke;

import java.io.*;

public class PanelPrintWriter extends PrintStream {
  static ByteArrayOutputStream baos = new ByteArrayOutputStream();
  Object panel;
  Luke luke;
  
  public PanelPrintWriter(Luke luke, Object panel) {
    super(baos);
    baos.reset();
    // retrieve previous text and separate it
    String text = luke.getString(panel, "text");
    if (text != null && text.length() > 0) {
      try {
        baos.write(text.getBytes());
        baos.write('\n');
        baos.write('\n');
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    this.panel = panel;
    this.luke = luke;
  }

  /* (non-Javadoc)
   * @see java.io.PrintWriter#println(java.lang.String)
   */
  @Override
  public void println(String x) {
    try {
      baos.write(x.getBytes());
      baos.write('\n');
    } catch (IOException e) {
      e.printStackTrace();
    }
    String text = new String(baos.toByteArray());
    luke.setString(panel, "text", text);
    luke.setInteger(panel, "start", text.length());
    luke.setInteger(panel, "end", text.length());
  }

}
