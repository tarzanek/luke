/**
 * 
 */
package org.getopt.luke.plugins;

public interface IOReporter {
  public void reportStatus(String msg);
  public void reportIO(String name, long bytes, boolean read);
}