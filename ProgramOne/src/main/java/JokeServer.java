import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class JokeServer {

  // Made these hard-coded values in the style of constants in Java to show intent and provide for readability

  private static final int Q_LENGTH = 6;
  private static final int PORT = 9001; // it's over 9000!

  private static final String JOKE_ONE = "Joke one";
  private static final String JOKE_TWO = "Joke two";
  private static final String JOKE_THREE = "Joke three";
  private static final String JOKE_FOUR = "Joke four";

  Collection<String> jokes = new ArrayList<>(Arrays.asList(JOKE_ONE, JOKE_TWO, JOKE_THREE, JOKE_FOUR));

  private boolean isJokeMode = false;  // if true = ProverbMode

  public static void main(String a[]) throws IOException {
    System.out.println("This is a partial version!");

    Socket socket;
    ServerSocket servsock = new ServerSocket(PORT, Q_LENGTH);
    System.out.println("Robert David's Joke Server 1.8 starting up, listening on port " + PORT + "\n");
    while (true) {
      socket = servsock.accept();
      // .start() will spawn a new thread and then call .run()
      new JokeWorker(socket).start();
    }
  }
}
class JokeWorker extends Thread {

  private static final String JOKE_ONE = "Joke one";
  private static final String JOKE_TWO = "Joke two";
  private static final String JOKE_THREE = "Joke three";
  private static final String JOKE_FOUR = "Joke four";

  Collection<String> jokes = new ArrayList<>(Arrays.asList(JOKE_ONE, JOKE_TWO, JOKE_THREE, JOKE_FOUR));
  private Socket sock;

  JokeWorker(Socket s) {
    sock = s;
  }

  public void run() {
    PrintStream outStream = null;
    BufferedReader in;
    try {
      in = new BufferedReader
              (new InputStreamReader(sock.getInputStream()));
      outStream = new PrintStream(sock.getOutputStream());
      try {
        String name;
        name = in.readLine();
        System.out.println("Looking up " + name);
        tellJoke(name, outStream, this.jokes);
      } catch (IOException e) {
        System.out.println("Server read error");
        e.printStackTrace();
      }
      sock.close();
    } catch (IOException e) {
      System.out.println(e);
    }
  }

  private void tellJoke(String name, PrintStream out, Collection<String> jokes) {
    Random rand = new Random(System.currentTimeMillis());   // Could look into ThreadLocalRandom when converting to ASync
    int idx = rand.nextInt(4);
    ArrayList<String> jokeList = (ArrayList<String>) jokes;
    String joke = jokeList.get(idx);
    out.println(joke);
  }

  private void parseMessage(String message) {

  }

  private Map<UUID, boolean[]> mapUUIDtoState(UUID uuid, boolean[] arr) {
    return null;
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
