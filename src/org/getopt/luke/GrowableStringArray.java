package org.getopt.luke;
/**
 * Simple Vector-like implementation of a growable String array.
 * @author Andrzej Bialecki
 */
public class GrowableStringArray {
  public int INITIAL_SIZE = 20;

  private int size = 0;

  private String[] array = null;

  public int size() {
    return size;
  }

  /**
   * Sets the value at specified index. If index is outside range the array is automatically
   * expanded.
   * @param index where to set the value
   * @param value
   */
  public void set(int index, String value) {
    if (array == null) array = new String[INITIAL_SIZE];
    if (array.length < index + 1) {
      String[] newArray = new String[index + INITIAL_SIZE];
      System.arraycopy(array, 0, newArray, 0, array.length);
      array = newArray;
    }
    if (index > size - 1) size = index + 1;
    array[index] = value;
  }

  /**
   * Appends the separator and value at specified index. If no value exists at the
   * specified position, this is equivalent to {@link #set(int, String)} - no separator
   * is appended in that case.
   * @param index selected position
   * @param sep separator
   * @param value value
   */
  public void append(int index, String sep, String value) {
    String oldVal = get(index);
    if (oldVal == null) {
      set(index, value);
    } else {
      set(index, oldVal + sep + value);
    }
  }

  /**
   * Return the value at specified index.
   * @param index
   * @return
   */
  public String get(int index) {
    if (array == null || index < 0 || index > array.length - 1) return null;
    return array[index];
  }
  
  public String toString(String separator) {
    StringBuffer sb = new StringBuffer();
    String sNull = "null";
    int k = 0, m = 0;
    for (int j = 0; j < size(); j++) {
      if (get(j) == null)
        k++;
      else {
        if (sb.length() > 0) sb.append(separator);
        if (m > 0 && m % 5 == 0) sb.append('\n');
        if (k > 0) {
          sb.append(sNull + "_" + k + separator);
          k = 0;
          m++;
        }
        sb.append(get(j));
        m++;
      }
    }
    return sb.toString();
  }
}