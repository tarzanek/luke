package org.getopt.luke.plugins;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexReader.FieldOption;
import org.apache.lucene.index.TermsEnum;
import org.getopt.luke.LukePlugin;
import org.getopt.luke.SlowThread;

import thinlet.Thinlet;

public class ZipfAnalysisPlugin extends LukePlugin {
  VocabChart chart = null;

  String selectedField;

  /** Default constructor. Initialize analyzers list. */
  public ZipfAnalysisPlugin() throws Exception {
  }

  public String getXULName() {
    return "/xml/zipf-plugin.xml";
  }

  public String getPluginName() {
    return "Zipf distributions";
  }

  public String getPluginInfo() {
    return "Tool for showing term popularity distributions, by Mark Harwood";
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
    // Object maxdoc = app.find(myUi, "maxdoc");
    // app.setString(maxdoc, "text", "");
    // Object middoc = app.find(myUi, "middoc");
    // app.setString(middoc, "text", "");
    Object bean = app.find(myUi, "vocabchart");
    Object container = app.getParent(bean);
    chart = new VocabChart(app, container);
    app.setComponent(bean, "bean", chart);
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
      // app.setString(maxdoc, "text", reader.maxDoc() + "");
      // app.setString(middoc, "text", (reader.maxDoc() / 2 ) + "");
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
    IndexReader reader = getIndexReader();
    if (reader == null) {
      app.showStatus("No index loaded");
      cleanChart();
      return;
    }
    SlowThread st = new SlowThread(app) {
      public void execute() {
        try {
          int numBuckets = 100;
          TermsEnum te = MultiFields.getTerms(ir, field).iterator();
          ArrayList terms = new ArrayList();

          // most terms occur very infrequently - just keep group totals for the DFs
          // representing these "long tail" terms.
          int longTailDfStart = 1;
          int longTailDfEnd = 1000;
          int longTailTermDfCounts[] = new int[(longTailDfEnd - longTailDfStart) + 1];

          // For "short tail" terms there are less of them and they represent a lot
          // of different
          // DFs (typically in the thousands) so unlike long-tail terms we can't
          // predict what common DF buckets to accumulate counts in. For this reason
          // we don't attempt
          // to total them and keep a list of them individually (shouldn't occupy
          // too much ram)

          int numUniqueTerms = 0;
          while (te.next() != null) {
            numUniqueTerms++;
            int df = te.docFreq();
            if (df <= longTailDfEnd) {
              int i = df - longTailDfStart;
              longTailTermDfCounts[i]++;
            } else {
              terms.add(new TermCount(new Term(field, te.term().utf8ToString()), df));
            }
          }

          TermCount sortedTerms[] = (TermCount[]) terms.toArray(new TermCount[terms
              .size()]);
          Arrays.sort(sortedTerms);

          int termsPerBucket;
          if (numUniqueTerms < 100) {
            termsPerBucket = 1;
            numBuckets = numUniqueTerms;
          } else {
            termsPerBucket = numUniqueTerms / numBuckets;
          }
          ArrayList buckets = new ArrayList();
          Bucket currentBucket = new Bucket();
          buckets.add(currentBucket);
          for (int i = 0; i < sortedTerms.length; i++) {
            currentBucket.addTermDf(sortedTerms[i].df);
            if (currentBucket.numTermsInThisBucket >= termsPerBucket) {
              // start a new bucket
              currentBucket = new Bucket();
              buckets.add(currentBucket);
            }
          }
          // now work through the aggregated long-tail terms - start from
          // most common DF down to least common DF
          for (int i = longTailTermDfCounts.length - 1; i >= 0; i--) {
            int df = i + longTailDfStart;
            int numTerms = longTailTermDfCounts[i];
            for (int t = 0; t < numTerms; t++) {
              currentBucket.addTermDf(df);
              if (currentBucket.numTermsInThisBucket >= termsPerBucket) {
                // start a new bucket
                currentBucket = new Bucket();
                buckets.add(currentBucket);
              }
            }
          }
          if (currentBucket.numTermsInThisBucket == 0) buckets.remove(currentBucket);
          Bucket bucketsResult[] = (Bucket[]) buckets.toArray(new Bucket[buckets.size()]);
          float termBucketTotals[] = new float[bucketsResult.length];
          int maxDf = 0;
          for (int i = 0; i < bucketsResult.length; i++) {
            termBucketTotals[i] = bucketsResult[i].getAverageDf();
            maxDf = (int) Math.max(maxDf, termBucketTotals[i]);
          }
          // update the GUI
          Object maxdf = app.find(myUi, "maxdf");
          app.setString(maxdf, "text", "" + maxDf);
          Object maxterm = app.find(myUi, "maxterm");
          Object midterm = app.find(myUi, "midterm");
          app.setString(maxterm, "text", numUniqueTerms + "");
          app.setString(midterm, "text", (numUniqueTerms / 2) + "");

          chart.setScores(termBucketTotals);
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

  static class Bucket {
    int maxDf;

    int minDf = Integer.MAX_VALUE;

    int totalDf;

    int numTermsInThisBucket;

    public void addTermDf(int df) {
      totalDf += df;
      numTermsInThisBucket++;
      maxDf = Math.max(df, maxDf);
      minDf = Math.min(df, minDf);
    }

    public int getAverageDf() {
      if (numTermsInThisBucket == 0)
        return 0;
      return totalDf / numTermsInThisBucket;
    }

    public String toString() {
      return maxDf + " maxDf in " + numTermsInThisBucket + " terms";
    }

  }

  static class TermCount implements Comparable {
    int df = 0;

    int termCount = 0;

    private Term term;

    public TermCount(Term term, int df) {
      super();
      this.term = term;
      this.df = df;
    }

    public int compareTo(Object o) {
      TermCount other = (TermCount) o;
      if (df > other.df) {
        return -1;
      }
      if (df < other.df) {
        return 1;
      }
      return 0;
    }
  }

}
