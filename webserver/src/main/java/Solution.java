import java.io.File;
import java.net.URISyntaxException;

public class Solution {
  public static void main(String[] args) {
    try {
      System.out.println(getDirectoryOfJAR());
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
  }

  // https://stackoverflow.com/a/320595
  private static String getDirectoryOfJAR() throws URISyntaxException {
    return new File(Solution.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
  }
}
