package org.getopt.luke;

import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

public class Progress implements Observer {
  Object ui;
  Object bar, msg;
  boolean showing = false;
  Luke luke;
  
  public Progress(Luke luke) {
    try {
      ui = luke.parse("/xml/progress.xml", this);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    this.luke = luke;
    bar = luke.find(ui, "bar");
    msg = luke.find(ui, "msg");
  }
  
  public void setMessage(String message) {
    luke.setString(msg, "text", message);
  }
  
  public void show() {
    luke.add(ui);
    luke.repaint();
    showing = true;
  }
  
  public void hide() {
    luke.remove(ui);
    showing = false;
  }
  
  public void cancel(Object dialog) {
    
  }

  public void update(Observable o, Object arg) {
    if (arg instanceof ProgressNotification) {
      ProgressNotification pn = (ProgressNotification)arg;
      if (pn.message != null) {
        luke.setString(msg, "text", pn.message);
      }
      luke.setInteger(bar, "minimum", pn.minValue);
      luke.setInteger(bar, "maximum", pn.maxValue);
      luke.setInteger(bar, "value", pn.curValue);
    } else {
      luke.setString(msg, "text", arg.toString());
    }
    if (!showing) {
      show();
    }
    luke.doLayout(ui);
  }
}
