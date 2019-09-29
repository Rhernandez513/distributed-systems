import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.UUID;

public class JokeClient {

  private static final UUID _UUID = UUID.randomUUID();

  // We define a constant in the java style
  private static final int PORT = 9001; // it's over 9000!

  public static void main(String args[]) {

    System.out.println("This is a partial version!");

    boolean [] state = { false, false, false, false } ;

    String machineName;

    // These variables do not change over the  course of execution so they are assigned to final
    // serverName was previously set as an if with no brackets, a bad idea in my eyes
    final String serverName = (args.length < 1) ? "localhost" : args[0];
    final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

    System.out.println("Robert David's Joke Client, 1.8.\n");
    System.out.println("Using server: " + serverName + ", Port: " + PORT);

    try {
      do {
        System.out.print("Enter your name, (quit) to end: ");
        System.out.flush();
        // This will read user input from the keyboard
        // Attempting to read Remote Address of the machine name supplied
        machineName = in.readLine();
        if (machineName.indexOf("quit") < 0) {
          getMessage(machineName, serverName, state);
        }
      } while (machineName.indexOf("quit") < 0); // Loop until the user wants to exit
      System.out.println("Cancelled by user request.");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // this function will call out to the remote host "serverName" and ask that
  // host to scan the local network for the provided "machineName"
  // printing the result of the communication to the console
  static void getMessage(String name, String serverName, boolean [] state) {
    String textFromServer;

    try {
      // these vars can be declared and initialized in one line each
      Socket sock = new Socket(serverName, PORT);
      // Create IO streams for the socket
      BufferedReader fromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));
      PrintStream toServer = new PrintStream(sock.getOutputStream());
      // Send hostname or IP to server
      final String messsage = "UUID: " + _UUID + ";STATE: " + state + ";";
      toServer.println(messsage);
      toServer.flush(); // don't put two statements on one line
      // Read three lines of response from the server, and block while synchronously waiting:
      for (int i = 1; i <= 3; i++) {
        // Don't assign vars inside loops if we can help it
        textFromServer = fromServer.readLine();
        // ALWAYS use brackets
        if (textFromServer != null) {
          System.out.println(textFromServer);
        }
      }
      // Here we close the external resource we acquired
      sock.close();
    } catch (IOException e) {
      System.out.println("Socket error.");
      e.printStackTrace();
    }
  }
}
