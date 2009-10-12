package org.getopt.luke.plugins;

/*
 * This is a slightly modified Shell from Rhino examples.
 */

/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * The contents of this file are subject to the Netscape Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/NPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * The Original Code is Rhino code, released
 * May 6, 1998.
 *
 * The Initial Developer of the Original Code is Netscape
 * Communications Corporation.  Portions created by Netscape are
 * Copyright (C) 1997-1999 Netscape Communications Corporation. All
 * Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the
 * terms of the GNU Public License (the "GPL"), in which case the
 * provisions of the GPL are applicable instead of those above.
 * If you wish to allow use of your version of this file only
 * under the terms of the GPL and not to allow others to use your
 * version of this file under the NPL, indicate your decision by
 * deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL.  If you do not delete
 * the provisions above, a recipient may use your version of this
 * file under either the NPL or the GPL.
 */

import org.mozilla.javascript.*;

import java.io.*;

/**
 * The shell program.
 * 
 * Can execute scripts interactively or in batch mode at the command line. An
 * example of controlling the JavaScript engine.
 * 
 * @author Norris Boyd
 */
public class Shell extends ImporterTopLevel {
  private PrintWriter stdout = null;

  private PrintWriter stderr = null;

  private InputStream stdin = null;

  public Shell(PrintWriter stdout, PrintWriter stderr, InputStream stdin) {
    this.stdin = stdin;
    this.stderr = stderr;
    this.stdout = stdout;
    String[] names = { "print", "quit", "version", "load", "help" };
    defineFunctionProperties(names, Shell.class, ScriptableObject.DONTENUM);
    putProperty(this, "stdout", stdout);
    putProperty(this, "stderr", stderr);
    putProperty(this, "stdin", stdin);
    stderr.println(Context.enter().getImplementationVersion());
    Context.exit();
  }

  public void destroy() {
    Context.exit();
  }
  
  public String getClassName() {
    return "global";
  }

  /**
   * Print a help message.
   * 
   * This method is defined as a JavaScript function.
   */
  public void help() {
    p("");
    p("Object                 Description");
    p("=======                ===========");
    p("app                    current Luke instance.");
    p("dir                    currently open Directory.");
    p("ir                     currently open IndexReader.");
    p("myUi                   this plugin's UI object.");
    p("");
    p("Command                Description");
    p("=======                ===========");
    p("defineClass(className) Define an extension using the Java class");
    p("                       named with the string argument. ");
    p("                       Uses ScriptableObject.defineClass(). ");
    p("load(['foo.js', ...])  Load JavaScript source files named by ");
    p("                       string arguments. ");
    p("loadClass(className)   Load a class named by a string argument.");
    p("                       The class must be a script compiled to a");
    p("                       class file. ");
    p("print([expr ...])      Evaluate and print expressions. ");
    p("version([number])      Get or set the JavaScript version number.");
  }

  /**
   * Print the string values of its arguments.
   * 
   * This method is defined as a JavaScript function. Note that its arguments
   * are of the "varargs" form, which allows it to handle an arbitrary number of
   * arguments supplied to the JavaScript function.
   *  
   */
  public static void print(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
    Object out = thisObj.get("stdout", thisObj);
    if (out == null || out == Scriptable.NOT_FOUND) return;
    PrintWriter writer = (PrintWriter) out;
    for (int i = 0; i < args.length; i++) {
      if (i > 0) writer.print(" ");

      // Convert the arbitrary JavaScript value into a string form.
      String s = Context.toString(args[i]);

      writer.print(s);
    }
    writer.println();
  }

  /**
   * Quit the shell.
   * 
   * This only affects the interactive mode.
   * 
   * This method is defined as a JavaScript function.
   */
  public void quit() {
    quitting = true;
  }

  /**
   * Get and set the language version.
   * 
   * This method is defined as a JavaScript function.
   */
  public static double version(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
    double result = (double) cx.getLanguageVersion();
    if (args.length > 0) {
      double d = cx.toNumber(args[0]);
      cx.setLanguageVersion((int) d);
    }
    return result;
  }

  /**
   * Load and execute a set of JavaScript source files.
   * 
   * This method is defined as a JavaScript function.
   *  
   */
  public static void load(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
    Shell shell = (Shell) getTopLevelScope(thisObj);
    for (int i = 0; i < args.length; i++) {
      shell.processSource(cx, new File(cx.toString(args[i])));
    }
  }

  public void prompt() {
    stderr.print("js> ");
    stderr.flush();
  }

  public Context getContext() {
    Context cx = Context.getCurrentContext();
    if (cx == null) {
      cx = Context.enter();
      cx.initStandardObjects(this);
      cx.setLanguageVersion(120);
    }
    return cx;
  }
  /**
   * Evaluate JavaScript source.
   * 
   * @param cx the current context
   * @param filename the name of the file to compile, or null for interactive
   *        mode.
   */
  public void processSource(Context cx, File filename) {
    FileReader in = null;
    try {
      in = new FileReader(filename);
    } catch (FileNotFoundException ex) {
      stderr.println("ERROR: Couldn't open file \"" + filename + "\".");
      return;
    }

    try {
      // Here we evalute the entire contents of the file as
      // a script. Text is printed only if the print() function
      // is called.
      cx.evaluateReader(this, in, filename.toString(), 1, null);
    } catch (WrappedException we) {
      stderr.println(we.getWrappedException().toString());
      we.printStackTrace();
    } catch (EvaluatorException ee) {
      stderr.println("js: " + ee.getMessage());
    } catch (JavaScriptException jse) {
      stderr.println("js: " + jse.getMessage());
    } catch (IOException ioe) {
      stderr.println(ioe.toString());
    } finally {
      try {
        in.close();
      } catch (IOException ioe) {
        stderr.println(ioe.toString());
      }
    }
    System.gc();
  }

  public void processSource(Context cx, String script) {
    try {
      Object result = cx.evaluateString(this, script, "<stdin>", 0, null);
      if (result != cx.getUndefinedValue()) {
        stderr.println(cx.toString(result));
      }
    } catch (WrappedException we) {
      // Some form of exception was caught by JavaScript and
      // propagated up.
      stderr.println(we.getWrappedException().toString());
      we.printStackTrace();
    } catch (EvaluatorException ee) {
      // Some form of JavaScript error.
      stderr.println("js: " + ee.getMessage());
    } catch (Exception jse) {
      // Some form of JavaScript error.
      stderr.println("js: " + jse.getMessage());
    }
    prompt();
    System.gc();
  }

  private void p(String s) {
    stdout.println(s);
  }

  private boolean quitting;
}

