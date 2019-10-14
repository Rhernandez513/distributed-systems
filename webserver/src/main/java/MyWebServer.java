import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.List;

class Constants {
  public static final String EOL = "\r\n";
}

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
          payload += s.replace("\n", "") + Constants.EOL;
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

    builder.append("HTTP/1.1 200 OK" + Constants.EOL);
    builder.append("Content-Length: " + contentLength + Constants.EOL);
    builder.append("Content-Type: " + contentType + Constants.EOL);
    builder.append(Constants.EOL);
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

class DynamicHTMLGenerator {
  public static void createCurrentTimeFile() {

    final String fileName = "system-time.html";
    final Timestamp currentTime = new Timestamp(System.currentTimeMillis());

    String workingDirectory = null;
    try {
      workingDirectory = WebServerFileUtil.getDirectoryOfJAR();
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }

    StringBuilder builder = new StringBuilder();
    builder.append("<html>" + Constants.EOL);
    builder.append("<body>" + Constants.EOL);
    builder.append("Current Time: " + currentTime  + "<p>" + Constants.EOL);
    builder.append("</body>" + Constants.EOL);
    builder.append("</html>" + Constants.EOL);

    final String absoluteFilePath = workingDirectory + FileSystems.getDefault().getSeparator() + fileName;
    try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(absoluteFilePath), StandardCharsets.UTF_8))) {
      writer.write(builder.toString());
    } catch (IOException e) {
      e.printStackTrace();
    }
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

    query = query.trim().split(" ")[1];

    if(query.contains("system-time.html")) {
      DynamicHTMLGenerator.createCurrentTimeFile();
    }

    try {
      currentPath = getDirectoryOfJAR();
      content = Files.readAllLines(Paths.get(currentPath + query));
    } catch (IOException | URISyntaxException e) {
      e.printStackTrace();
    }

    return content;
  }

  // https://stackoverflow.com/a/320595
  public synchronized static String getDirectoryOfJAR() throws URISyntaxException {
    return new File(MyWebServer.class.getProtectionDomain().getCodeSource().getLocation().toURI())
            .getPath();
  }
}
