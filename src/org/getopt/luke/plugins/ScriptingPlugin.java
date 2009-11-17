/*
 * Created on Feb 7, 2005
 * Author: Andrzej Bialecki &lt;ab@getopt.org&gt;
 *
 */
package org.getopt.luke.plugins;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.getopt.luke.Luke;
import org.getopt.luke.LukePlugin;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tools.shell.Main;

import thinlet.Thinlet;

/**
 * This is a JavaScript interactive shell. All elements
 * of Luke framework are made available to the plugin by putting
 * them into JS Context.
 * 
 * @author Andrzej Bialecki &lt;ab@getopt.org&gt;
 */
public class ScriptingPlugin extends LukePlugin {

  private Shell shell = null;
  private StringBuffer scroll = new StringBuffer();


  public ScriptingPlugin() {
  }

  public void reset() {
    if (shell != null) shell.destroy();
    Object ta = app.find(myUi, "console");
    app.setString(ta, "text", "");
    scroll.setLength(0);
    StringWriter writer = new StringWriter();
    TAWriter taw = new TAWriter(app, ta, scroll, writer);
    // Associate a new Context with this thread
    try {
      // Initialize the standard objects (Object, Function, etc.)
      // This must be done before scripts can be executed.
      shell = new Shell(taw, taw, System.in);
      ScriptableObject.putProperty(shell, "app", app);
      ScriptableObject.putProperty(shell, "ir", ir);
      ScriptableObject.putProperty(shell, "dir", dir);
      ScriptableObject.putProperty(shell, "myUi", myUi);
      shell.prompt();
    } catch (Exception e) {
      e.printStackTrace();
      app.errorMsg("Error initializing Shell:\n" + e.getMessage());
    }
  }

  public void clear() {
    scroll.setLength(0);
    scroll.append("js> ");
    Object console = app.find(myUi, "console");
    app.setString(console, "text", scroll.toString());
    app.requestFocus(console);
    app.setInteger(console, "start", scroll.length());
    app.setInteger(console, "end", scroll.length());
  }
  public String getXULName() {
    return "/xml/scr-plugin.xml";
  }

  public String getPluginName() {
    return "Scripting Luke";
  }

  public String getPluginInfo() {
    return "Luke Scripting Plugin; by Andrzej Bialecki";
  }

  public String getPluginHome() {
    return "mailto:ab@getopt.org";
  }

  public boolean init() throws Exception {
    if (shell == null) reset();
    ScriptableObject.putProperty(shell, "app", app);
    ScriptableObject.putProperty(shell, "ir", ir);
    ScriptableObject.putProperty(shell, "dir", dir);
    ScriptableObject.putProperty(shell, "myUi", myUi);
    return true;
  }
  
  public void setWrap(Object cbWrap, Object ta) {
    app.setBoolean(ta, "wrap", app.getBoolean(cbWrap, "selected"));
  }
  
  public void execute(String cmd) {
    //System.out.println("exec '" + cmd + "'");
    scroll.append(cmd);
    if (!cmd.endsWith("\n")) scroll.append("\n");
    shell.processSource(shell.getContext(), cmd.trim());
    Object console = app.find(myUi, "console");
    app.requestFocus(console);
    app.setInteger(console, "start", scroll.length());
    app.setInteger(console, "end", scroll.length());
  }
  
  public void ins(Object ta) {
    String text = app.getString(ta, "text");
    if (!text.substring(0, scroll.length()).equals(scroll.toString())) {
      text = scroll.toString() + text.substring(scroll.length() + 1);
      app.setString(ta, "text", text);
      app.setInteger(ta, "start", text.length());
      app.setInteger(ta, "end", text.length());
    }
    String cmd = text.substring(scroll.length());
    int idx = cmd.indexOf('\n');
    if (cmd.endsWith("\n") && shell.getContext().stringIsCompilableUnit(cmd)) execute(cmd);
  }
  
  public void rem(Object ta) {
    int start = app.getInteger(ta, "start");
    if (start < scroll.length()) {
      app.setString(ta, "text", scroll.toString());
      app.setInteger(ta, "start", scroll.length());
      app.setInteger(ta, "end", scroll.length());
    }    
  }
  public void car(Object ta) {
    int start = app.getInteger(ta, "start");
    if (start < scroll.length()) {
      app.setInteger(ta, "start", scroll.length());
      app.setInteger(ta, "end", scroll.length());
    }
  }
  
  public void actionHelp(Object ta) {
    String cmd = "help()\n";
    execute(cmd);
  }
  
  public void actionSample() {
    StringBuffer sb = new StringBuffer();
    try {
      InputStream is = getClass().getResourceAsStream("/xml/SampleScript.js");
      BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
      String line = null;
      while ((line = br.readLine()) != null) {
        sb.append(line + "\n");
      }
      br.close();
      execute(sb.toString());
    } catch (Exception e) {
      e.printStackTrace();
      app.errorMsg("Loading failed: " + e.getMessage());
      return;
    }      
  }
}

class TAWriter extends PrintWriter {
  public Luke app;
  public Object ta;
  public StringBuffer scroll;
  public StringWriter writer;

  public TAWriter(Luke app, Object ta, StringBuffer scroll, StringWriter writer) {
    super(writer);
    this.ta = ta;
    this.scroll = scroll;
    this.app = app;
    this.writer = writer;
  }
  
  
  public void flush() {
    super.flush();
    scroll.append(writer.getBuffer());
    app.setString(ta, "text", scroll.toString());
    writer.getBuffer().setLength(0);
  }
}