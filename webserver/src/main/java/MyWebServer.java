import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class MyWebServer {

  private static final int Q_LENGTH = 6; /* Number of requests for OpSys to queue */
  private static final int PORT = 2540;

  public static void main(String[] args) {
    try {

      ServerSocket servsock = new ServerSocket(PORT, Q_LENGTH);
      while (true) {
        // Will block on .accept()
        new Thread(new WebServerWorker(servsock.accept())).start();
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

class WebServerWorker implements Runnable {
  private Socket socket;
  static final String EOL = "\r\n";
  private String ContentType = null;

  WebServerWorker(Socket socket) {
    this.socket = socket;
  }

  public void run() {
    try (BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
      String line;
      List<String> fileContents = null;
      PrintStream outputStream = new PrintStream(socket.getOutputStream(), true);
      do {
        line = input.readLine();
        if (line == null) {
          break;
        }
        if (line.startsWith("GET")) {
          ContentType = WebServerFileUtil.determineFileType(line);
          fileContents = WebServerFileUtil.getFileContents(line);
        }
        System.out.println(line.trim());
      } while (line != null && !line.equals(""));
      // Here we construct the payload to sent back to the client w/ the required CRLF
      if (fileContents != null) {
        String payload = "";
        for(String s : fileContents) {
          // Convert LF line endings to CLRF
          payload += s.replace("\n", "") + EOL;
        }
        // Respond to client
        final String response = buildResponse(ContentType, payload);
        outputStream.print(response);
        outputStream.flush();
        // Debugging only
        System.out.println("Printing response");
        System.out.print(response);
        System.out.flush();
      }
      // Close our opened resource
      outputStream.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private String buildResponse(String contentType, String payload) {

    StringBuilder builder = new StringBuilder();

    final int contentLength = determineContentLength(payload);

    builder.append("HTTP/1.1 200 OK" + EOL);
    builder.append("Content-Length: " + contentLength + EOL);
    builder.append("Content-Type: " + contentType + EOL);
    builder.append(EOL);
    builder.append(payload);

    return builder.toString();
  }

  private int determineContentLength(String payload) {

    int contentLength = -1;

    try {
      contentLength = payload.getBytes("UTF-8").length;
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }

    return contentLength;
  }
}

class WebServerFileUtil {

  public static String determineFileType(String query) {

    final String HTML = "text/html";
    final String PLAINTEXT = "text/plain";
    final String JSON = "application/json";

    query = query.split(" ")[1];
    if (query.endsWith(".html")) {
      return HTML;
    } else if (query.endsWith(".txt") || query.endsWith(".java")) {
      return PLAINTEXT;
    } else if (query.endsWith(".json")) {
      return JSON;
    }

    return null;
  }

  public static List<String> getFileContents(String query) {

    String currentPath = null;
    List<String> content = null;

    query = query.trim();
    String[] lineArr = query.split(" ");

    try {
      currentPath = getDirectoryOfJAR();
      content = Files.readAllLines(Paths.get(currentPath + lineArr[1]));
    } catch (IOException | URISyntaxException e) {
      e.printStackTrace();
    }

    return content;
  }

  // https://stackoverflow.com/a/320595
  private synchronized static String getDirectoryOfJAR() throws URISyntaxException {
    return new File(MyWebServer.class.getProtectionDomain().getCodeSource().getLocation().toURI())
            .getPath();
  }
}
