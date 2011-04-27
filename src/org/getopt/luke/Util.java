package org.getopt.luke;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.HashMap;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;

public class Util {
  
  public static String xmlEscape(String in) {
    if (in == null) return "";
    StringBuilder sb = new StringBuilder(in.length());
    for (int i = 0; i < in.length(); i++) {
      char c = in.charAt(i);
      switch (c) {
      case '&':
        sb.append("&amp;");
        break;
      case '>':
        sb.append("&gt;");
        break;
      case '<':
        sb.append("&lt;");
        break;
      case '"':
        sb.append("&quot;");
        break;
      case '\'':
        sb.append("&#039;");
        break;
      default:
        sb.append(c);
      }
    }
    return sb.toString();
  }

  public static String bytesToHex(byte bytes[], int offset, int length, boolean wrap) {
    StringBuffer sb = new StringBuffer();
    boolean newLine = false;
    for (int i = offset; i < offset + length; ++i) {
      if (i > offset && !newLine) {
        sb.append(" ");
      }
      sb.append(Integer.toHexString(0x0100 + (bytes[i] & 0x00FF))
                       .substring(1));
      if (i > 0 && (i + 1) % 16 == 0 && wrap) {
        sb.append("\n");
        newLine = true;
      } else {
        newLine = false;
      }
    }
    return sb.toString();
  }

  private static final byte[] EMPTY_BYTES = new byte[0];

  public static byte[] hexToBytes(String hex) {
    if (hex == null) {
      return EMPTY_BYTES;
    }
    hex = hex.replaceAll("\\s+", "");
    if (hex.length() == 0) {
      return EMPTY_BYTES;
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream(hex.length() / 2);
    byte b;
    for (int i = 0; i < hex.length(); i++) {
      int high = charToNibble(hex.charAt(i));
      int low = 0;
      if (i < hex.length() - 1) {
        i++;
        low = charToNibble(hex.charAt(i));
      }
      b = (byte)(high << 4 | low);
      baos.write(b);
    }
    return baos.toByteArray();
  }

  static final int charToNibble(char c) {
    if (c >= '0' && c <= '9') {
      return c - '0';
    } else if (c >= 'a' && c <= 'f') {
      return 0xa + (c - 'a');
    } else if (c >= 'A' && c <= 'F') {
      return 0xA + (c - 'A');
    } else {
      throw new RuntimeException("Not a hex character: '" + c + "'");
    }
  }

  public static String byteToHex(byte b) {
    return bytesToHex(new byte[]{b}, 0, 1, false);
  }

  public static String escape(String[] values, String sep) {
    if (values == null) return null;
    if (values.length == 0) return "";
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < values.length; i++) {
      if (i > 0) sb.append(sep);
      sb.append(escape(values[i]));
    }
    return sb.toString();
  }

  public static String escape(String value) {
    if (value == null) return null;
    if (value.length() == 0) return value;
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch(c) {
        case '\t':
          sb.append("\\t");
          break;
        case '\r':
          sb.append("\\r");
          break;
        case '\n':
          sb.append("\\n");
          break;
        default:
          if (((int)c >= 32 && (int)c <= 127) || Character.isLetterOrDigit(c)) {
            sb.append(c);
          } else {
            sb.append("&#" + (int)c + ";");
          }
      }
    }
    return sb.toString();
  }
  
  public static String fieldFlags(Field f) {
    if (f == null) {
      return "-----------";
    }
    StringBuffer flags = new StringBuffer();
    if (f != null && f.isIndexed()) flags.append("I");
    else flags.append("-");
    if (f != null && f.isTokenized()) flags.append("T");
    else flags.append("-");
    if (f != null && f.isStored()) flags.append("S");
    else flags.append("-");
    if (f != null && f.isTermVectorStored()) flags.append("V");
    else flags.append("-");
    if (f != null && f.isStoreOffsetWithTermVector()) flags.append("o");
    else flags.append("-");
    if (f != null && f.isStorePositionWithTermVector()) flags.append("p");
    else flags.append("-");
    if (f != null && f.getOmitTermFreqAndPositions()) flags.append("f");
    else flags.append("-");
    if (f != null && f.getOmitNorms()) flags.append("O");
    else flags.append("-");
    if (f != null && f.isLazy()) flags.append("L");
    else flags.append("-");
    if (f != null && f.isBinary()) flags.append("B");
    else flags.append("-");
    return flags.toString();
  }
  
  public static Resolution getResolution(String key) {
    if (key == null || key.trim().length() == 0) {
      return Resolution.MILLISECOND;
    }
    Resolution r = resolutionMap.get(key);
    if (r != null) return r;
    return Resolution.MILLISECOND;
  }

  private static HashMap<String, Resolution> resolutionMap = new HashMap<String, Resolution>();
  static {
    resolutionMap.put(Resolution.MILLISECOND.toString(), Resolution.MILLISECOND);
    resolutionMap.put(Resolution.SECOND.toString(), Resolution.SECOND);
    resolutionMap.put(Resolution.MINUTE.toString(), Resolution.MINUTE);
    resolutionMap.put(Resolution.HOUR.toString(), Resolution.HOUR);
    resolutionMap.put(Resolution.DAY.toString(), Resolution.DAY);
    resolutionMap.put(Resolution.MONTH.toString(), Resolution.MONTH);
    resolutionMap.put(Resolution.YEAR.toString(), Resolution.YEAR);
  }
  
  public static long calcTotalFileSize(String path, Directory fsdir) throws Exception {
    long totalFileSize = 0L;
    String[] files = null;
    if (fsdir instanceof FSDirectory) {
      files = ((FSDirectory)fsdir).listAll();
    } else if (fsdir instanceof MMapDirectory) {
      files = ((MMapDirectory)fsdir).listAll();
    }
    if (files == null) return totalFileSize;
    for (int i = 0; i < files.length; i++) {
      String filename;
      if (path.endsWith(File.separator)) {
        filename = path;
      } else {
        filename = path + File.separator;
      }
  
      File file = new File(filename + files[i]);
      totalFileSize += file.length();
    }
    return totalFileSize;
  }

  public static String normalizeUnit(long len) {
    if (len == 1) {
      return " byte";
    } else if (len < 1024) {
      return " bytes";
    } else if (len < 51200000) {
      return " kB";
    } else {
      return " MB";
    }
  }

  public static String normalizeSize(long len) {
    if (len < 1024) {
      return String.valueOf(len);
    } else if (len < 51200000) {
      return String.valueOf(len / 1024);
    } else {
      return String.valueOf(len / 1048576);
    }
  }
}
