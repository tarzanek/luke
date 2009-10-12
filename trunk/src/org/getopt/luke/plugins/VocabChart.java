package org.getopt.luke.plugins;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;

import org.getopt.luke.Luke;

public class VocabChart extends Component {

  Image offScrImg = null;

  int offScrWidth;

  int offScrHeight;

  private float[] scores;

  private int xBorder = 2;

  private int yBorder = 2;

  private int minRowHeight = 20;

  float max = 0;
  float total = 0;
  
  Luke app = null;
  Object container = null;

  public VocabChart() {
  }

  public VocabChart(Luke app, Object container) {
    this.app = app;
    this.container = container;
  }

  public Dimension getPreferredSize() {
    if (app == null) return new Dimension(500, 300);
    return app.getSize(container, 2, 2);
  }

  public void update(Graphics rg) {
    paint(rg);
  }

  public void paint(Graphics rg) {
    Dimension d = getSize();
    // Hmmm. createImage returns null in Thinlet
    // offScrImg = createImage(getWidth(), getHeight());
    offScrWidth = d.width;
    offScrHeight = d.height;
    int availWidth = offScrWidth - (xBorder * 2);
    int availHeight = offScrHeight - (yBorder * 2);
    Graphics og = rg;
    og.setColor(Color.white);
    og.fillRect(0, 0, getWidth(), getHeight());
    if (scores == null) {
      return;
    }

    int numRows = 1;
    while ((scores.length > (availWidth * numRows))
        && (((numRows * yBorder) + (numRows * minRowHeight)) < availHeight)) {
      numRows++;
    }
    int rowHeight = (availHeight / numRows) - yBorder;
    float x = xBorder;
    float y = yBorder + rowHeight;

    // paint row background
    /*og.setColor(Color.RED);
    for (int i = 0; i < numRows; i++) {
      og.fillRect((int) x, (int) y - rowHeight, availWidth, rowHeight);
      y += rowHeight + yBorder;
    }*/

    // paint match values
    float pixelsPerBar = (float) (numRows * availWidth) / (float) scores.length;
    int intPixelsPerBar = Math.max(1, (int) pixelsPerBar - 1);
    x = xBorder;
    y = yBorder + rowHeight;

    for (int i = 0; i < scores.length; i++) {
      og.setColor(Color.BLUE);
      if (scores[i] > 0) {
        int height = (int) ((float) rowHeight * (scores[i] / max));
        og.fillRect((int) x, (int) y - height, intPixelsPerBar, height);
      }
      og.setColor(Color.BLACK);
      int len = 2;
      if (i % 5 == 0) len = 4;
      og.drawLine((int)x, (int)y, (int)x, (int)y + len);
      x += pixelsPerBar;
      if (x > availWidth) {
        x = xBorder;
        y += rowHeight + yBorder;
      }
    }
  }

  public int getMinRowHeight() {
    return minRowHeight;
  }

  public void setMinRowHeight(int minRowHeight) {
    this.minRowHeight = minRowHeight;
  }

  public float[] getScores() {
    return scores;
  }

  public void setScores(float[] scores) {
    this.scores = scores;
    max = 0;
    total = 0;
    if (scores != null) {
      for (int i = 0; i < scores.length; i++) {
        max = Math.max(scores[i], max);
        total += scores[i];
      }
    }
  }

  public int getXBorder() {
    return xBorder;
  }

  public void setXBorder(int border) {
    xBorder = border;
  }

  public int getYBorder() {
    return yBorder;
  }

  public void setYBorder(int border) {
    yBorder = border;
  }

}
