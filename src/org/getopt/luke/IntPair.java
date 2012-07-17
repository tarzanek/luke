/*
 *
 * Created on January 22, 2003, 12:13 PM
 */

package org.getopt.luke;

/**
 *
 * @author  Administrator
 */
public class IntPair {
    
    public int[] positions;
    public int[] starts;
    public int[] ends;
    public int cnt = 0;
    public String text = null;
    
    public IntPair(int cnt, String text) {
        this.cnt = cnt;
        this.text = text;
    }

    public String toString() {
        return cnt + ":'" + text + "'";
    }
    
    public static class PairComparator implements java.util.Comparator<IntPair> {
        private boolean ascending;
        private boolean byText;
        
        public PairComparator(boolean byText, boolean ascending) {
            this.ascending = ascending;
            this.byText = byText;
        }
        
        public int compare(IntPair h1, IntPair h2) {
            if (byText) {
                return ascending?h1.text.compareTo(h2.text):h2.text.compareTo(h1.text);
            } else {
                if (h1.cnt > h2.cnt) return ascending?-1:1;
                if (h1.cnt < h2.cnt) return ascending?1:-1;
            }
            return 0;
        }
    }
}
