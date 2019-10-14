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
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

class Constants {
  public static final String EOL = "\r\n";
  public static final String SECURITY_ERROR_FILE = "security-error.html";
  public static final String SYSTEM_TIME_FILE = "system-time.html";
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
      List<String> fileContents = new ArrayList<>();
      PrintStream outputStream = new PrintStream(socket.getOutputStream(), true);

      boolean directoryFlag = false;

      do {
        line = input.readLine();
        if (line == null) {
          break;
        }
        // Respond to GET request flow
        if (line.startsWith("GET")) {

          String queryString = line.split(" ")[1];

          // Security disallowing "jumping" the root dir
          if(line.contains("..") || line.contains("security-error.html")) {
            line = Constants.SECURITY_ERROR_FILE;
          } else if (queryString.endsWith("/") || queryString.length() == 1) {
            // We want to look at a directory
            directoryFlag = true;
            String directoryContents = WebServerFileUtil.readAllFilesInDir(queryString);
            String formattedContents = DynamicHTMLGenerator.createDirectoryListingFile(directoryContents, queryString);
//            fileContents.add(directoryContents);
            fileContents.add(formattedContents);
          }

          if (directoryFlag == false) {
            // Actions for reading a file
            ContentType = WebServerFileUtil.determineFileType(line);
            fileContents = WebServerFileUtil.getFileContents(line);
          }

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

  // Allows for thread safe interaction w/ the filesystem for writing out files
  private static synchronized void writeOutFile(String absolutePath, String payload) {
    try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(absolutePath), StandardCharsets.UTF_8))) {
      writer.write(payload);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // this function deals with dynamically creating web pages that list files and directories
  public static String createDirectoryListingFile(String payload, String queryString) {

    String header = "<h3> Index of: " + queryString + "</h3>";

    StringBuilder output = new StringBuilder();

    output.append(header + Constants.EOL);

    final String fileFormatPrefix = "[TXT] <a href=\"";
    final String fileFormatSuffixOne = "\">";
    final String fileFormatSuffixTwo = "</a>";

    final String dirFormatPrefix = "[DIR] <a href=\"";
    final String dirFormatSuffixOne = "\">";
    final String dirFormatSuffixTwo = "</a>";

    final String[] lines = payload.split(Constants.EOL);

    for (String s : lines) {
      if (s.contains("File")) {
        String fileName = s.split(" ")[1];
        if (fileName.contains(".class")) {
          // Exclude compiled java bytecode
          continue;
        }
        // Exclude data info from href
        fileName = fileName.substring(0, fileName.indexOf('('));
        output.append(fileFormatPrefix);

        // This has to do with nested directories, only tested with one level of nesting
        if (fileName.contains(queryString.substring(1))) {
          String [] _s = fileName.split("/");
          output.append(_s[_s.length - 1]);
        } else {
          output.append(queryString + fileName);
        }
        output.append(fileFormatSuffixOne);

        output.append(fileName);
        output.append(fileFormatSuffixTwo);
      } else if (s.contains("Directory")) {
        String dirName = queryString + s.split(" ")[1] + "/";
        output.append(dirFormatPrefix);
        output.append(dirName);
        output.append(dirFormatSuffixOne);
        output.append(dirName.substring(1));
        output.append(dirFormatSuffixTwo);
      }
      output.append("</p>");
    }

    return buildHTML(output.toString());
  }

  // Dynamically creates the ACCESS DENIED page
  public static void createSecurityErrorFile() {
    final String fileName = Constants.SECURITY_ERROR_FILE;

    String workingDirectory = null;
    try {
      workingDirectory = WebServerFileUtil.getDirectoryOfJAR();
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }

    final String absoluteFilePath = workingDirectory + FileSystems.getDefault().getSeparator() + fileName;
    final String content = "<h1>ACCESS DENIED.  DIRECTORY TRAVERSAL DISALLOWED.</h1>";

    writeOutFile(absoluteFilePath, buildHTML(content));
  }

  // Dynamically creates system-time.html
  public static void createCurrentTimeFile() {

    final String fileName = Constants.SYSTEM_TIME_FILE;
    final Timestamp currentTime = new Timestamp(System.currentTimeMillis());

    String workingDirectory = null;
    try {
      workingDirectory = WebServerFileUtil.getDirectoryOfJAR();
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }

    final String absoluteFilePath = workingDirectory + FileSystems.getDefault().getSeparator() + fileName;
    final String content = "Current Time: " + currentTime  + "<p>";

    writeOutFile(absoluteFilePath, buildHTML(content));
  }
  private static String buildHTML(String content) {

    StringBuilder builder = new StringBuilder();

    builder.append("<html>" + Constants.EOL);
    builder.append("<body>" + Constants.EOL);
    builder.append(content + Constants.EOL);
    builder.append("</body>" + Constants.EOL);
    builder.append("</html>" + Constants.EOL);

    return builder.toString();
  }
}

class WebServerFileUtil {

// https://condor.depaul.edu/elliott/435/hw/programs/mywebserver/ReadFiles.java
  public static synchronized String readAllFilesInDir(String request) {


    final String dir;
    final String separator = FileSystems.getDefault().getSeparator();
    String syntheticRoot;
    StringBuilder builder = new StringBuilder();

    try {
      syntheticRoot = getDirectoryOfJAR() + separator;
      if (request.length() == 1) {
        dir = getDirectoryOfJAR();
      } else {
        dir = getDirectoryOfJAR() + separator + request;
      }
      File root = new File(dir);
      File[] contents = root.listFiles();
      for (File f : contents) {
        String _f = f.toString().substring(syntheticRoot.length());
        if (f.isDirectory()) {
//          builder.append("Directory: " + f + Constants.EOL);
          builder.append("Directory: " + _f + Constants.EOL);
        } else if (f.isFile()) {
          // This will expose the FULL PATH on your server, be careful using this on the real web
//          builder.append("File: " + f + "(" + f.length() +")" + Constants.EOL);
          builder.append("File: " + _f + "(" + f.length() +")" + Constants.EOL);
        } else {
          builder.append("Something went wrong with: " + f + Constants.EOL);
        }
      }
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
    return builder.toString();
  }

  public static String determineFileType(String query) {

    final String HTML = "text/html";
    final String PLAINTEXT = "text/plain";
    final String JSON = "application/json";

    if (query.contains(Constants.SECURITY_ERROR_FILE)) {
      return HTML;
    }

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

    boolean security_flag = false;
    if(query.contains(Constants.SECURITY_ERROR_FILE)) {
      DynamicHTMLGenerator.createSecurityErrorFile();
      security_flag = true;
    } else if(query.contains(Constants.SYSTEM_TIME_FILE)) {
      DynamicHTMLGenerator.createCurrentTimeFile();
    }

    if (security_flag == false) {
      query = query.trim().split(" ")[1];
    } else {
      query = FileSystems.getDefault().getSeparator() + query;
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
