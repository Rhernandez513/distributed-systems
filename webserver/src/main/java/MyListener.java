import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class MyListener {

  private static final int Q_LENGTH = 6; /* Number of requests for OpSys to queue */
  private static final int PORT = 2540;

  public static void main(String[] args) {
    try {

      ServerSocket servsock = new ServerSocket(PORT, Q_LENGTH);
      while (true) {
        // Will block on .accept()
        new Thread(new ListenerWorker(servsock.accept())).start();
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

class ListenerWorker implements Runnable {
  private Socket socket;
  ListenerWorker (Socket socket) {
    this.socket = socket;
  }

  public void run() {
    try (BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
      String line;
      List<String> fileContents = null;
      do {
        line = input.readLine();
        if (line == null) {
          break;
        }
        if (line.startsWith("GET")) {
          fileContents = FileUtil.getFile(line);
        }
        System.out.println(line.trim());
      } while (line != null);
      if (fileContents != null) {
        for(String s : fileContents) {
          System.out.println(s);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

class FileUtil {
  public static List<String> getFile(String line) {

    line = line.trim();

    String[] lineArr = line.split(" ");

    String currentPath = null;
    List<String> content = null;
    try {
      currentPath = getDirectoryOfJAR();
      content = Files.readAllLines(Paths.get(currentPath + lineArr[1]));
    } catch (IOException | URISyntaxException e) {
      e.printStackTrace();
    }
    return content;
  }
  // https://stackoverflow.com/a/320595
  private static String getDirectoryOfJAR() throws URISyntaxException {
    return new File(MyWebServer.class.getProtectionDomain().getCodeSource().getLocation().toURI())
            .getPath();
  }
}
