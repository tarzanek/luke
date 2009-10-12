package org.getopt.luke.plugins;

import java.util.Collection;
import java.util.Iterator;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.IndexReader.FieldOption;
import org.getopt.luke.LukePlugin;
import org.getopt.luke.SlowThread;

import thinlet.Thinlet;

public class VocabAnalysisPlugin extends LukePlugin {
  VocabChart chart = null;

  String selectedField;

  /** Default constructor. Initialize analyzers list. */
  public VocabAnalysisPlugin() throws Exception {
  }

  public String getXULName() {
    return "/xml/vocab-plugin.xml";
  }

  public String getPluginName() {
    return "Vocabulary Analysis Tool";
  }

  public String getPluginInfo() {
    return "Tool for showing index's vocabulary growth, by Mark Harwood";
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
    Object combobox = app.find(myUi, "fields");
    app.removeAll(combobox);
    Object maxdoc = app.find(myUi, "maxdoc");
    app.setString(maxdoc, "text", "");
    Object middoc = app.find(myUi, "middoc");
    app.setString(middoc, "text", "");
    Object bean = app.find(myUi, "vocabchart");
    Object container = app.getParent(bean);
    chart = new VocabChart(app, container);
    IndexReader reader = getIndexReader();
    if (reader != null) {
      Collection fieldNames = reader.getFieldNames(FieldOption.INDEXED);
      String firstField = null;
      for (Iterator iter = fieldNames.iterator(); iter.hasNext();) {
        String fieldName = (String) iter.next();
        if (firstField == null) {
          firstField = fieldName;
        }
        Object choice = Thinlet.create("choice");
        app.setString(choice, "text", fieldName);
        app.add(combobox, choice);
      }
      app.setInteger(combobox, "selected", 0);
      app.setString(combobox, "text", firstField);
      app.setComponent(bean, "bean", chart);
      app.setString(maxdoc, "text", reader.maxDoc() + "");
      app.setString(middoc, "text", (reader.maxDoc() / 2 ) + "");
    }
    return true;
  }
  
  public void cleanChart() {
    chart.setScores(null);
    chart.invalidate();
    app.repaint();
  }

  public void analyze() {
    Object combobox = app.find(myUi, "fields");
    final String field = app.getString(combobox, "text");
    Object ckbox = app.find(myUi, "cumul");
    final boolean cumul = app.getBoolean(ckbox, "selected");
    IndexReader reader = getIndexReader();
    if (reader == null) {
      app.showStatus("No index loaded");
      cleanChart();
      return;
    }
    SlowThread st = new SlowThread(app) {
      public void execute() {
        try {
          int numAgeGroups = 100;
          float numDocs = ir.maxDoc();
          if (numDocs < numAgeGroups) numAgeGroups = ir.maxDoc();
          float ageTotals[] = new float[numAgeGroups];
          String internedField = field.intern();
          TermEnum te = ir.terms(new Term(internedField, ""));
          Term term = te.term();
          while (term != null) {
            if (internedField != term.field()) {
              break;
            }
            TermDocs td = ir.termDocs(term);
            td.next();
            float firstDocId = td.doc();
            int ageBracket = (int) ((firstDocId / numDocs) * numAgeGroups);
            ageTotals[ageBracket]++;
            if (te.next()) {
              term = te.term();
            } else {
              term = null;// ends loop
            }
          }
          float total = 0.0f;
          float max = 0.0f;
          for (int i = 0; i < ageTotals.length; i++) {
            if (ageTotals[i] > max) max = ageTotals[i];
            total += ageTotals[i];
            if (i > 0 && cumul) {
              ageTotals[i] += ageTotals[i - 1]; // make totals cumulative
            }
          }
          Object maxpct = app.find(myUi, "maxpct");
          if (cumul) {
            app.setString(maxpct, "text", "100 %");
          } else {
            app.setString(maxpct, "text", (float)Math.round(max * 10000.0f / total) / 100.0f + " %");
          }
          chart.setScores(ageTotals);
          chart.invalidate();
          app.repaint();
        } catch (Exception e) {
          app.showStatus("ERROR: " + e.getMessage());
        }        
      }
    };
    if (app.isSlowAccess()) {
      st.start();
    } else {
      st.execute();
    }
  }

  public String getSelectedField() {
    return selectedField;
  }

  public void setSelectedField(String selectedField) {
    this.selectedField = selectedField;
  }

}