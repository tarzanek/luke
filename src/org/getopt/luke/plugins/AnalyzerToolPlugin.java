package org.getopt.luke.plugins;

import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.util.Iterator;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.FlagsAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.index.Payload;
import org.apache.lucene.util.Attribute;
import org.apache.lucene.util.AttributeSource;
import org.apache.lucene.util.Version;
import org.getopt.luke.LukePlugin;
import org.getopt.luke.Util;

public class AnalyzerToolPlugin extends LukePlugin {

  /** Default constructor. Initialize analyzers list. */
  public AnalyzerToolPlugin() throws Exception {
  }

  public String getXULName() {
    return "/xml/at-plugin.xml";
  }

  public String getPluginName() {
    return "Analyzer Tool";
  }

  public String getPluginInfo() {
    return "Tool for analyzing analyzers, by Mark Harwood";
  }

  public String getPluginHome() {
    return "mailto:mharwood@apache.org";
  }

  /** Overriden to populate the drop down even if no index is open. */
  public void setMyUi(Object ui) {
    super.setMyUi(ui);
    try {
      init();
    } catch (Exception e) {
      e.printStackTrace();
    }
    ;
  }

  public boolean init() throws Exception {
    Object combobox = app.find(myUi, "analyzers");
    app.removeAll(combobox);
    String firstClass = "";
    Class[] analyzers = app.getAnalyzers();
    for (int i = 0; i < analyzers.length; i++) {
      Object choice = app.create("choice");
      app.setString(choice, "text", analyzers[i].getName());
      if (i == 0) {
        firstClass = analyzers[i].getName();
      }
      app.add(combobox, choice);
    }
    app.setInteger(combobox, "selected", 0);
    app.setString(combobox, "text", firstClass);
    Object aVersion = app.find(myUi, "aVersion");
    app.removeAll(aVersion);
    Version[] values = Version.values();
    for (int i = 0; i < values.length; i++) {
      Version v = values[i];
      Object choice = app.create("choice");
      app.setString(choice, "text", v.toString());
      app.putProperty(choice, "version", v);
      app.add(aVersion, choice);
      if (v.equals(Version.LUCENE_CURRENT)) {
        app.setInteger(aVersion, "selected", i);
      }
    }
    return true;
  }
  
  public void analyze() {
    try {
      Object combobox = app.find(myUi, "analyzers");
      Object resultsList = app.find(myUi, "resultsList");
      Object inputText = app.find(myUi, "inputText");
      String classname = app.getString(combobox, "text");
      Object choice = app.getSelectedItem(app.find(myUi, "aVersion"));
      Version v = (Version)app.getProperty(choice, "version");
      Class clazz = Class.forName(classname);
      Analyzer analyzer = null;
      try {
        Constructor<Analyzer> c = clazz.getConstructor(Version.class);
        analyzer = c.newInstance(v);
      } catch (Throwable t) {
        try {
          // no constructor with Version ?
          analyzer = (Analyzer)clazz.newInstance();
        } catch (Throwable t1) {
          t1.printStackTrace();
          app
                .showStatus("Couldn't instantiate analyzer - public 0-arg or 1-arg constructor(Version) required");
          return;
        }
      }
      TokenStream ts = analyzer.tokenStream("text", new StringReader(app
              .getString(inputText, "text")));
      app.removeAll(resultsList);

      while (ts.incrementToken()) {
        Object row = app.create("item");
        app.setString(row, "text", ((CharTermAttribute)ts.getAttribute(CharTermAttribute.class)).toString());
        app.add(resultsList, row);
        app.putProperty(row, "state", ts.cloneAttributes());
      }
    } catch (Throwable t) {
      app.showStatus("Error analyzing:" + t.getMessage());
    }
    tokenChange();
  }

  public void tokenChange() {
    Object table = app.find(myUi, "tokenAtts");
    app.removeAll(table);
    Object list = app.find("resultsList");
    Object row = app.getSelectedItem(list);
    if (row == null) {
      return;
    }
    AttributeSource as = (AttributeSource)app.getProperty(row, "state");
    if (as == null) {
      return;
    }
    Iterator it = as.getAttributeClassesIterator();
    while (it.hasNext()) {
      Class cl = (Class)it.next();
      String  attClass = cl.getName();
      if (attClass.startsWith("org.apache.lucene.")) {
        attClass = cl.getSimpleName();
      }
      Attribute att = as.getAttribute(cl);
      String implClass = att.getClass().getName();
      if (implClass.startsWith("org.apache.lucene.")) {
        implClass = att.getClass().getSimpleName();
      }
      Object r = app.create("row");
      Object cell = app.create("cell");
      app.add(table, r);
      app.add(r, cell);
      app.setString(cell, "text", attClass);
      cell = app.create("cell");
      app.add(r, cell);
      app.setString(cell, "text", implClass);
      cell = app.create("cell");
      app.add(r, cell);
      String val = null;
      if (attClass.equals("TermAttribute")) {
        val = ((TermAttribute)att).term();
      } else if (attClass.equals("FlagsAttribute")) {
        val = Integer.toHexString(((FlagsAttribute)att).getFlags());
      } else if (attClass.equals("OffsetAttribute")) {
        OffsetAttribute off = (OffsetAttribute)att;
        val = off.startOffset() + "," + off.endOffset();
      } else if (attClass.equals("PayloadAttribute")) {
        Payload payload = ((PayloadAttribute)att).getPayload();
        if (payload != null) {
          byte[] data = payload.getData();
          val = Util.bytesToHex(data, 0, data.length, false);
        } else {
          val = "";
        }
      } else if (attClass.equals("PositionIncrementAttribute")) {
        val = ((PositionIncrementAttribute)att).getPositionIncrement() + "";
      } else if (attClass.equals("TypeAttribute")) {
        val = ((TypeAttribute)att).type();
      } else {
        val = att.toString();
      }
      app.setString(cell, "text", val);
    }
    Object inputText = app.find(myUi, "inputText");
    if (as.hasAttribute(OffsetAttribute.class)) {
      OffsetAttribute off = (OffsetAttribute)as.getAttribute(OffsetAttribute.class);
      app.setInteger(inputText, "start", 0);
      app.setInteger(inputText, "end", off.endOffset());
      app.setInteger(inputText, "start", off.startOffset());
      app.requestFocus(inputText);
    }
  }
}