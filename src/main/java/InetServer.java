import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

class Worker extends Thread {
  private Socket socket;
  Worker (Socket s) {
    this.socket = s;
  }

  public void run() {
    PrintStream outStream = null;
    BufferedReader inReader = null;
    try {
      inReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      outStream = new PrintStream(socket.getOutputStream());
      try {
        String name;
        name = inReader.readLine();
        System.out.println("Looking up " + name);
        printRemoteAddress(name, outStream);
      } catch (IOException e) {
        System.out.println("Server read error");
        e.printStackTrace();
      }
      socket.close();
    } catch (IOException e) {
      System.out.println(e);
    }

  }

  static void printRemoteAddress(String name, PrintStream outStream) {
    try {
      outStream.println("Looking up " + name + "...");
      InetAddress machine = InetAddress.getByName(name);
      outStream.println("Host name : " + machine.getHostName());
      outStream.println("Host IP : " + toText(machine.getAddress()));
    } catch (UnknownHostException e) {
      outStream.println("Failed to attempt to look up " + name);
    }
  }

  static String toText(byte ip[]) {
    final StringBuffer result = new StringBuffer();
    for(int i = 0; i < ip.length; ++i) {
      if (i > 0) {
        result.append(0xff & ip[i]);
      }
    }
    return result.toString();
  }
}

public class InetServer {
  private static final int Q_LENGTH = 6;
  private static final int PORT = 1565;

  public static void main(String[] args) throws IOException {
    Socket socket;

    ServerSocket serversocket = new ServerSocket(PORT, Q_LENGTH);

    System.out.println("Robert David's Inet server 1.8 starting up, listening on port " + PORT + "\n");
    while (true) {
      socket = serversocket.accept();
      new Worker(socket).start();
    }
  }
}

// EOF

