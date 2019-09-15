package common;

public class TextUtil {
  // Makes portable for 128 bit format
  public static String toText(byte ip[]) {
    final StringBuffer result = new StringBuffer();
    for (int i = 0; i < ip.length; ++i) {
      if (i > 0) {
        result.append(".");
      }
      result.append(0xff & ip[i]);
    }
    return result.toString();
  }
}
