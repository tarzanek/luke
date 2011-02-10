package org.getopt.luke.plugins;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

import javax.swing.JFileChooser;

import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.DefaultSimilarity;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Explanation.IDFExplanation;
import org.getopt.luke.LukePlugin;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * This plugin lets you write your own implementation of Similarity
 * using JavaScript and Mozilla Rhino engine.
 * <p>The script needs to provide functions with specific signatures.
 * These functions are then executed by a CustomSimilarity facade class.</p>
 * 
 * @author Andrzej Bialecki &lt;ab@getopt.org&gt;
 */
public class SimilarityDesignerPlugin extends LukePlugin {
  
    private CustomSimilarity similarity = null;
    private JFileChooser fd = null;

    /** Default constructor. */
    public SimilarityDesignerPlugin() throws Exception {
      fd = new JFileChooser();
      fd.setFileSelectionMode(JFileChooser.FILES_ONLY);
      fd.setFileHidingEnabled(false);
      fd.setCurrentDirectory(new File(System.getProperty("user.dir")));
    }

    public String getXULName() {
        return "/xml/sd-plugin.xml";
    }
    
    public String getPluginName() {
        return "Custom Similarity";
    }
    
    public String getPluginInfo() {
        return "Custom Similarity Designer; by Andrzej Bialecki";
    }
    
    public String getPluginHome() {
        return "mailto:ab@getopt.org";
    }
    
    public boolean init() throws Exception {
      if (similarity != null) app.setCustomSimilarity(similarity);
      return true;
    }
    
    public void actionLoadSample(Object menu) {
      StringBuffer sb = new StringBuffer();
      String name = app.getString(menu, "text");
      try {
        InputStream is = getClass().getResourceAsStream("/xml/" + name);
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        String line = null;
        while ((line = br.readLine()) != null) {
          sb.append(line + "\n");
        }
        br.close();
        Object simText = app.find(myUi, "simText");
        app.setString(simText, "text", sb.toString());
        actionCompileSimilarity();
      } catch (Exception e) {
        e.printStackTrace();
        app.errorMsg("Loading failed: " + e.getMessage());
        return;
      }      
    }
    
    public void actionNew() {
      Object simText = app.find(myUi, "simText");
      app.setString(simText, "text", "");
      invalidate();
    }
    
    public void invalidate() {
      app.setCustomSimilarity(null);
      Object simStatus = app.find(myUi, "simStatus");
      app.setString(simStatus, "text", "Needs Compile");
      app.setColor(simStatus, "foreground", Color.red);
    }
    
    public void actionCompileSimilarity() {
      Object simText = app.find(myUi, "simText");
      String script = app.getString(simText, "text");
      Object cbCompInt = app.find(myUi, "cbCompInt");
      int level;
      if (app.getBoolean(cbCompInt, "selected"))
        level = -1;
      else
        level = 9;
      Context cx = Context.enter();
      cx.setOptimizationLevel(level);
      PrintStream out = null;
      Object selOutput = app.find(myUi, "selOutput");
      String sel = app.getString(selOutput, "text");
      if (sel.equals("System.out"))
        out = System.out;
      else if (sel.equals("System.err"))
        out = System.err;
      Object simStatus = app.find(myUi, "simStatus");
      try {
        ScriptableObject scope = cx.initStandardObjects();
        cx.evaluateString(scope, script, "<cmd>", 1, null);
        if (similarity != null) similarity.destroy();
        similarity = new CustomSimilarity(cx, scope, out);
        app.setCustomSimilarity(similarity);
        app.setString(simStatus, "text", "OK");
        app.setColor(simStatus, "foreground", Color.green);
      } catch (Exception e) {
        e.printStackTrace();
        app.setCustomSimilarity(null);
        app.setString(simStatus, "text", "ERROR");
        app.setColor(simStatus, "foreground", Color.red);
        app.errorMsg("Compile failed:\n" + e.getMessage());
      }
    }
    
    public void actionSaveFile() {
      fd.setDialogType(JFileChooser.SAVE_DIALOG);
      fd.setDialogTitle("Select Output File");
      int res = fd.showOpenDialog(app);
      File file = null;
      if (res == JFileChooser.APPROVE_OPTION) file = fd.getSelectedFile();
      if (file == null || file.isDirectory()) return;
      try {
        FileOutputStream fos = new FileOutputStream(file);
        Object simText = app.find(myUi, "simText");
        String script = app.getString(simText, "text");
        fos.write(script.getBytes("UTF-8"));
        fos.flush();
        fos.close();
        app.showStatus("Saved OK.");
      } catch (Exception e) {
        e.printStackTrace();
        app.errorMsg("Could not save '" + file.toString() + ": " + e.getMessage());
        return;
      }
    }
    
    public void actionOpenFile() {
      fd.setDialogType(JFileChooser.OPEN_DIALOG);
      fd.setDialogTitle("Select Input File");
      int res = fd.showOpenDialog(app);
      File file = null;
      if (res == JFileChooser.APPROVE_OPTION) file = fd.getSelectedFile();
      if (file == null || file.isDirectory()) return;
      try {
        BufferedReader br = new BufferedReader(
              new InputStreamReader(new FileInputStream(file), "UTF-8"));
        StringBuffer sb = new StringBuffer();
        String line = null;
        while ((line = br.readLine()) != null) {
          sb.append(line + "\n");
        }
        br.close();
        Object simText = app.find(myUi, "simText");
        app.setString(simText, "text", sb.toString());
      } catch (Exception e) {
        e.printStackTrace();
        app.errorMsg("Could not open '" + file.toString() + ": " + e.getMessage());
        return;
      }
    }
}

/**
 * This class provides a facade to JavaScript functions. All
 * abstract methods of Similarity are required to be implemented by the
 * scripts. All non-abstract (and non-static) methods MAY be overriden,
 * but if they are not a default implementation is used.
 *  
 * @author Andrzej Bialecki &lt;ab@getopt.org&gt;
 */
class CustomSimilarity extends DefaultSimilarity {
  private static final int M_A_COORD      = 0;
  private static final int M_A_IDF        = 1;
  private static final int M_A_LENGTHNORM = 2;
  private static final int M_A_QUERYNORM  = 3;
  private static final int M_A_SLOPPYFREQ = 4;
  private static final int M_A_TF         = 5;
  private static final int M_A_MAX        = 6;

  private static final int M_IDF_CS       = 0;
  private static final int M_IDF_TS       = 1;
  private static final int M_TF_I         = 2;
  private static final int M_MAX          = 3;

  private String[] abstractIds = {
    "coord",
    "idf",
    "lengthNorm",
    "queryNorm",
    "sloppyFreq",
    "tf"
  };
  
  private String[] otherIds = {
    "idf_cs",
    "idf_ts",
    "tf_i"
  };
  
  private Function[] abstractMethods = new Function[M_A_MAX];
  private Function[] otherMethods = new Function[M_MAX];
  
  private ScriptableObject scope = null;
  private Context cx = null;
  
  public CustomSimilarity(Context cx, ScriptableObject scope, PrintStream printStream) throws Exception {
    this.scope = scope;
    this.cx = cx;
    for (int i = 0; i < abstractIds.length; i++) {
      Object m = scope.get(abstractIds[i], scope);
      if (m == null || m == cx.getUndefinedValue() || m == Scriptable.NOT_FOUND) {
        throw new Exception("Required abstract method '"
                + abstractIds[i] + "' is missing.");
      }
      if (!(m instanceof Function))
        throw new Exception("Symbol '" + abstractIds[i] + "' is not a function.");
      abstractMethods[i] = (Function)m;
    }
    for (int i = 0; i < otherIds.length; i++) {
      Object m = scope.get(otherIds[i], scope);
      if (m == null || m == cx.getUndefinedValue() || m == Scriptable.NOT_FOUND) continue;
      if (!(m instanceof Function))
        throw new Exception("Symbol '" + otherIds[i] + "' is not a function, but'" + m + "'");
      otherMethods[i] = (Function)m;
    }
    scope.defineFunctionProperties(new String[]{"print"}, CustomSimilarity.class, ScriptableObject.DONTENUM);
    scope.putProperty(scope, "stdout", printStream);
  }
  
  public static void print(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
    Object ps = thisObj.get("stdout", thisObj);
    if (ps == null || !(ps instanceof PrintStream)) return;
    PrintStream printStream = (PrintStream)ps;
    for (int i = 0; i < args.length; i++) {
      if (i > 0) printStream.print(" ");

      // Convert the arbitrary JavaScript value into a string form.
      String s = Context.toString(args[i]);

      printStream.print(s);
    }
    printStream.println();
  }
  
  public void destroy() {
    cx.exit();
  }
  
  // A
  @Override
  public float coord(int arg0, int arg1) {
    Object[] args = new Object[]{new Integer(arg0), new Integer(arg1)};
    Object res = abstractMethods[M_A_COORD].call(cx, scope, scope, args);
    float f = 0.0f;
    try {
      f = Float.parseFloat(res.toString());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return f;
  }
  
  private static class FakeIDFExplanation extends IDFExplanation {
    float idf;
    public FakeIDFExplanation(float idf) {
      this.idf = idf;
    }

    @Override
    public String explain() {
      return "fake explanation";
    }

    @Override
    public float getIdf() {
      return idf;
    }
    
  }
  
  @Override
  public IDFExplanation idfExplain(Term term, IndexSearcher searcher)
          throws IOException {
    int numDocs = searcher.maxDoc();
    int docFreq = searcher.docFreq(term);
    Object[] args = new Object[]{new Integer(docFreq), new Integer(numDocs)};
    Object res = abstractMethods[M_A_IDF].call(cx, scope, scope, args);
    if (res instanceof Number) { // back-compat
      return new FakeIDFExplanation(((Number)res).floatValue());
    } else {
      return (IDFExplanation)res;      
    }
  }

  // A
  public float computeNorm(String field, FieldInvertState state) {
    Object[] args = new Object[]{field, new Integer(state.getLength())};
    Object res = abstractMethods[M_A_LENGTHNORM].call(cx, scope, scope, args);
    float f = 0.0f;
    try {
      f = Float.parseFloat(res.toString());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return f;
  }
  
  // A
  public float queryNorm(float arg0) {
    Object[] args = new Object[]{new Float(arg0)};
    Object res = abstractMethods[M_A_QUERYNORM].call(cx, scope, scope, args);
    float f = 0.0f;
    try {
      f = Float.parseFloat(res.toString());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return f;
  }
  
  // A
  public float sloppyFreq(int arg0) {
    Object[] args = new Object[]{new Integer(arg0)};
    Object res = abstractMethods[M_A_SLOPPYFREQ].call(cx, scope, scope, args);
    float f = 0.0f;
    try {
      f = Float.parseFloat(res.toString());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return f;
  }
  
  // A
  public float tf(float arg0) {
    Object[] args = new Object[]{new Float(arg0)};
    Object res = abstractMethods[M_A_TF].call(cx, scope, scope, args);
    float f = 0.0f;
    try {
      f = Float.parseFloat(res.toString());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return f;
  }
  
  public float tf(int arg0) {
    Function func = otherMethods[M_TF_I];
    if (func == null) return super.tf(arg0);
    Object[] args = new Object[]{new Integer(arg0)};
    Object res = func.call(cx, scope, scope, args);
    float f = 0.0f;
    try {
      f = Float.parseFloat(res.toString());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return f;
  }
}