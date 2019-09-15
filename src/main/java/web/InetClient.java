package web;// Based on web.InetClient, a client for web.InetServer. Elliot, after Huges, Shoffner, Winslow

// DePaul CSC 435, Autumn 2019
// Robert David Hernandez, rherna57@mail.depaul.edu

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

public class InetClient {

  private static final int PORT = 1565;

  public static void main(String[] args) {
    String serverName, name;

    if (args.length < 1) { // always use brackets, always.  Not using brackets is a bug waiting to happen
      serverName = "localhost";
    } else {
      serverName = args[0];
    }

    System.out.println("Robert David's Inet Client, 1.8.\n");
    System.out.println("Using server: " + serverName + ", Port: " + PORT);
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    try {
      do {
        System.out.print("Enter a hostname or an IP address, (quit) to end: ");
        System.out.flush();
        // This will read user input from the keyboard
        name = in.readLine();
        if (name.indexOf("quit") < 0) {
          getRemoteAddress(name, serverName);
        }
      } while (name.indexOf("quit") < 0);
      System.out.println("Cancelled by user request.");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  static void getRemoteAddress(String name, String serverName) {
    Socket socket;
    BufferedReader fromServer;
    PrintStream toServer;
    String textFromServer;

    try {
      // Open connection to to host:PORT
      socket = new Socket(serverName, PORT);
      // Create filer IO streams for the socket
      fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      toServer = new PrintStream(socket.getOutputStream());

      // Send hostname or IP to server
      toServer.println(name);
      toServer.flush(); // I would never advise putting two statements on one line

      // Read two or three lines of response from the server,
      // and block while synchronously waiting:
      for(int i = 0; i <= 3; ++i) {
        // This will
        textFromServer = fromServer.readLine();
        if (textFromServer != null) { // always use brackets, always
          System.out.println(textFromServer);
        }
        socket.close();
      }
    } catch (IOException e) {
      System.out.println("Socket error.");
      e.printStackTrace();
    }
  }
}
