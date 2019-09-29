import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ConnectException;
import java.net.Socket;

public class JokeServerAdmin {
  // We define a constant in the java style
  private static final int PORT = 5050;

  public static void main(String args[]) {
    System.out.println("This is a partial version!");

    String machineName;

    // These variables do not change over the  course of execution so they are assigned to final
    // serverName was previously set as an if with no brackets, a bad idea in my eyes
    final String serverName = (args.length < 1) ? "localhost" : args[0];
    final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

    System.out.println("Robert David's JokeClientAdmin, 1.8.\n");
    System.out.println("Using server: " + serverName + ", Port: " + PORT);

    try {
      do {
        System.out.print("1. Press Return to Toggle\n" +
                         "2. Enter \"shutdown\" to turn off the JokeServer\n" +
                         "3. Enter \"quit\" to end: ");
        System.out.flush();
        // This will read user input from the keyboard
        // Attempting to read Remote Address of the machine name supplied
        machineName = in.readLine();
        if (machineName.equals("shutdown")) {
          sendMessage("shutdown", serverName);
        } else {
          sendMessage("toggle", serverName);
        }
      } while (machineName.indexOf("quit") < 0); // Loop until the user wants to exit
      System.out.println("Cancelled by user request.");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  static void sendMessage(String payload, String serverName) {
    try {
      Socket sock = new Socket(serverName, PORT);
      // Create IO streams for the socket
      PrintStream toServer = new PrintStream(sock.getOutputStream());
      toServer.println(payload);
      toServer.flush();
      // We do not attempt to read a response from the server, we "fire and forget"
      // Here we close the external resource we acquired
      sock.close();
    } catch (ConnectException e) {
      // Sometimes we see this on one "quit" even though it works fine
    } catch (IOException e) {
      System.out.println("Socket error.");
      e.printStackTrace();
    }
  }
}
