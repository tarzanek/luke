package org.getopt.luke;

public class LimitedException extends RuntimeException {
  private int limitType;
  private long limitSize;
  private int lastDoc;
  private long currentSize;
  
  public LimitedException(int limitType, long limitSize, long currentSize, int lastDoc) {
    super();
    this.limitType = limitType;
    this.limitSize = limitSize;
    this.currentSize = currentSize;
    this.lastDoc = lastDoc;
  }

  /**
   * @return the limitType
   */
  public int getLimitType() {
    return limitType;
  }

  /**
   * @param limitType the limitType to set
   */
  public void setLimitType(int limitType) {
    this.limitType = limitType;
  }

  /**
   * @return the limitSize
   */
  public long getLimitSize() {
    return limitSize;
  }

  /**
   * @param limitSize the limitSize to set
   */
  public void setLimitSize(long limitSize) {
    this.limitSize = limitSize;
  }

  /**
   * @return the currentSize
   */
  public long getCurrentSize() {
    return currentSize;
  }

  /**
   * @param currentSize the currentSize to set
   */
  public void setCurrentSize(long currentSize) {
    this.currentSize = currentSize;
  }

  /**
   * @return the lastDoc
   */
  public int getLastDoc() {
    return lastDoc;
  }

  /**
   * @param lastDoc the lastDoc to set
   */
  public void setLastDoc(int lastDoc) {
    this.lastDoc = lastDoc;
  }
}
