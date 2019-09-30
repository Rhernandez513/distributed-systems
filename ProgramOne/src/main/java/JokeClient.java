import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

public class JokeClient {

  // We define a constant in the java style
  private static final int PORT = 4545; // it's over 9000!
  private static final int SECONDARY_PORT = 4546;

  private static boolean [] jokeState = { false, false, false, false } ;
  private static boolean [] proverbState = { false, false, false, false } ;

  public static void main(String args[]) {

    System.out.println("This is a partial version!");

    String userName = "";

    // These variables do not change over the  course of execution so they are assigned to final
    // serverName was previously set as an if with no brackets, a bad idea in my eyes
    final String serverName = (args.length < 1) ? "localhost" : args[0];
    final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

    System.out.println("Robert David's Joke Client, 1.8.\n");
    System.out.println("Using server: " + serverName + ", Port: " + PORT);

    try {
      String prompt;
      String userInput = "";
      boolean first_run = true;
      do {
        if (first_run) {
          prompt = "Enter your name, (quit) to end: ";
        } else {
          prompt = "Press return to get another message (if the Server just switched modest , you might have to press Return twice), (quit) to end: ";
        }
        System.out.print(prompt);
        System.out.flush();
        if (first_run) {
          userName = in.readLine();
          first_run = false;
        } else {
          userInput = in.readLine();
        }
        if (userInput.indexOf("quit") < 0) {
          getMessage(userName, serverName);
        }
      } while (userName.indexOf("quit") < 0 && userInput.indexOf("quit") < 0); // Loop until the user wants to exit
    System.out.println("Cancelled by user request.");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // this function will call out to the remote host "serverName" and ask that
  // host to scan the local network for the provided "machineName"
  // printing the result of the communication to the console
  static void getMessage(String name, String serverName) {
    String textFromServer;
    String serverMode = "";

    try {
      // these vars can be declared and initialized in one line each
      Socket sock = new Socket(serverName, PORT);
      // Create IO streams for the socket
      BufferedReader fromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));
      PrintStream toServer = new PrintStream(sock.getOutputStream());

      final String message = name + ";" + serializeState(jokeState) + ";" + serializeState(proverbState);
      toServer.println(message);
      toServer.flush(); // don't put two statements on one line
      // Read four lines from server and block while waiting
      for(int i = 0; i < 4; ++i) {
        textFromServer = fromServer.readLine();
        if (textFromServer == null) {
          // End of output from server
          break;
        } else if (textFromServer.startsWith("MODE: ")) {
          serverMode = deSerializeMode(textFromServer);
          continue; // we avoid printing what mode the server is in
        } else if (textFromServer.startsWith("STATE: ")) {
          boolean[] returnedState = deSerializeState(textFromServer);
          if (serverMode.equals("JOKE")) {
            JokeClient.jokeState = returnedState;
          } else if (serverMode.equals("PROVERB")) {
            JokeClient.proverbState = returnedState;
          }
          continue; // we avoid printing state to the console for now
        }
        // We print out the message that isn't state related
        System.out.println(textFromServer);
      }
      // Here we close the external resource we acquired
      sock.close();
    } catch (IOException e) {
      System.out.println("Socket error.");
      e.printStackTrace();
    }
  }
  static String serializeState(boolean[] state) {
    String stateAsString = "";
    for(boolean b : state) {
      if (b) {
        stateAsString += "true,";
      } else {
        stateAsString += "false,";
      }
    }
    return stateAsString;
  }

  private static String deSerializeMode(String mode) {
    return mode.substring(6); // Trimming off "MODE: "
  }

  private static boolean[] deSerializeState(String state) {
    boolean[] stateAsBools = new boolean[4];
    state = state.substring(7); // Trimming off "STATE: "
    String[] content = state.trim().split(",");

    for(int i = 0; i < content.length; ++i) {
      content[i] = content[i].trim();
    }

    for(int i = 0; i < stateAsBools.length; ++i) {
      if (content[i].equals("true")) {
        stateAsBools[i] = true;
      } else {
        stateAsBools[i] = false;
      }
    }
    return stateAsBools;
  }
}
