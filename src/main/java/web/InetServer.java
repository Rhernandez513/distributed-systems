package web;// Based on web.InetServer. Elliot, after Huges, Shoffner, Winslow
// DePaul CSC 435, Autumn 2019
// Robert David Hernandez, rherna57@mail.depaul.edu

// I prefer to be explicit rather than implicit, but I can see value in wildcard imports
// If the list is large enough to make it a distraction

import common.TextUtil;

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
  // Moved variables used in multiple methods to class scope in the object-oriented style
  private PrintStream outStream;

  Worker(Socket s) {
    this.socket = s;
  }

  public void run() {
    // Assignment to null is unnecessary in this instance
    // we could even move this var to class level for consistency, but it isn't used outside this method
    BufferedReader inReader;

    try {
      inReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      outStream = new PrintStream(socket.getOutputStream());
      try {
        final String name = inReader.readLine();
        System.out.println("Looking up " + name);
        printRemoteAddress(name);
      } catch (IOException e) {
        System.out.println("Server read error");
        e.printStackTrace();
      }
      socket.close();
    } catch (IOException e) {
      System.out.println(e);
    }
  }

  // We aren't using another module inside the same package, so static use of these
  // two methods with default visibility doesn't add value
  private void printRemoteAddress(String name) {
    try {
      outStream.println("Looking up " + name + "...");
      InetAddress machine = InetAddress.getByName(name);
      outStream.println("Host name : " + machine.getHostName());
      outStream.println("Host IP : " + TextUtil.toText(machine.getAddress()));
    } catch (UnknownHostException e) {
      outStream.println("Failed to attempt to look up " + name);
    }
  }
}

public class InetServer {
  // Made these hard-coded values in the style of constants in Java to show intent and provide for
  // readability
  private static final int Q_LENGTH = 6;
  private static final int PORT = 1565;

  public static void main(String[] args) throws IOException {
    Socket socket;

    final ServerSocket serversocket = new ServerSocket(PORT, Q_LENGTH);

    System.out.println("Robert David's Inet server 1.8 starting up, listening on port " + PORT + "\n");
    while (true) {
      socket = serversocket.accept();
      // I did an internet search and found that .start() will spawn a new thread
      // and then call .run() where a direct call to .run() will not spawn a thread
      new Worker(socket).start();
    }
  }
}

// EOF

