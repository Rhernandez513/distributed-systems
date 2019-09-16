package web; // Based on InetServer Elliot, after Huges, Shoffner, Winslow

// DePaul CSC 435, Autumn 2019
// Robert David Hernandez, rherna57@mail.depaul.edu

// I prefer to be explicit rather than implicit, but I can see value in wildcard imports
// If the list is large enough to make it a distraction

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;


class Worker extends Thread {
  private Socket sock;

  Worker(Socket s) {
    sock = s;
  }

  public void run() {
    // tried moving this to class level to avoid passing it around but it didn't work out, undoing
    PrintStream outStream = null;
    // Assignment to null is unnecessary in this instance
    // we could even move this var to class level for consistency, but it isn't used outside this method
    BufferedReader in;
    try {
      in = new BufferedReader
              (new InputStreamReader(sock.getInputStream()));
      outStream = new PrintStream(sock.getOutputStream());
      try {
        String name;
        name = in.readLine();
        System.out.println("Looking up " + name);
        printRemoteAddress(name, outStream);
      } catch (IOException e) {
        System.out.println("Server read error");
        e.printStackTrace();
      }
      sock.close();
    } catch (IOException e) {
      System.out.println(e);
    }
  }

  // We aren't using another module inside the same package, so static use of these
  // two methods with default visibility doesn't add value
  private void printRemoteAddress(String name, PrintStream out) {
    try {
      out.println("Looking up " + name + "...");
      InetAddress machine = InetAddress.getByName(name);
      out.println("Host name : " + machine.getHostName());
      out.println("Host IP : " + toText(machine.getAddress()));
    } catch (UnknownHostException ex) {
      out.println("Failed in atempt to look up " + name);
    }
  }

  // Makes portable for 128 bit format
  static String toText(byte ip[]) {
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

public class InetServer {
  // Made these hard-coded values in the style of constants in Java to show intent and provide for readability
  private static final int Q_LENGTH = 6;
  private static final int PORT = 9001; // it's over 9000!

  public static void main(String a[]) throws IOException {
    Socket socket;
    ServerSocket servsock = new ServerSocket(PORT, Q_LENGTH);
    System.out.println("Robert David's Inet server 1.8 starting up, listening on port " + PORT + "\n");
    while (true) {
      socket = servsock.accept();
      // .start() will spawn a new thread and then call .run()
      new Worker(socket).start();
    }
  }
}

