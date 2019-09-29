import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

public class JokeServer {

  // Made these hard-coded values in the style of constants in Java to show intent and provide for
  // readability

  private static final int Q_LENGTH = 6; /* Number of requests for OpSys to queue */
  private static final int CLIENT_PORT = 9001; // it's over 9000!
  private static final int ADMIN_PORT = 5050;

  private static final String JOKE_ONE = "Joke one";
  private static final String JOKE_TWO = "Joke two";
  private static final String JOKE_THREE = "Joke three";
  private static final String JOKE_FOUR = "Joke four";

  Collection<String> jokes =
      new ArrayList<>(Arrays.asList(JOKE_ONE, JOKE_TWO, JOKE_THREE, JOKE_FOUR));

  static volatile Boolean jokeMode = true; // if true = JokeMode

  public static void main(String a[]) throws IOException {

    boolean adminControlSwitch = true;

    System.out.println("This is a partial version!");

    System.out.println("Robert David's Joke Server 1.8 starting up, listening on admin port " + ADMIN_PORT + "\n");
    // I tried my best to use a ASyncSocketChannel but couldn't get it to work using 2 ports, only 1 port
    // I will probably try again next time around.  I was having issue interrupting threads, and running out of memory from creating
    // multiple threads.  I tried a few ways to limit the amount of threads spawning, but it didn't alleviate the earlier issues
    Thread adminLoopThread = new Thread( () -> {
      System.out.println("In the admin looper thread");
       try (ServerSocket adminServSock = new ServerSocket(ADMIN_PORT, Q_LENGTH)) {
         while (adminControlSwitch) {
           // wait for the next ADMIN client connection:
           new AdminWorker(adminServSock.accept()).start();
         }
       } catch (IOException ioe) {
         System.out.println(ioe);
       }
     });
    adminLoopThread.start();

    ServerSocket servsock = new ServerSocket(CLIENT_PORT, Q_LENGTH);
    System.out.println( "Robert David's Joke Server 1.8 starting up, listening on client port " + CLIENT_PORT + "\n");
    while (true) {
      // this will block until a connection is accepted and then spawn a new thread
      // .start() will spawn a new thread and then call .run()
      new JokeWorker(servsock.accept()).start();
    }
  }
}

class AdminWorker extends Thread {

  private final Socket socket;

  AdminWorker(Socket socket) {
    this.socket = socket;
  }

  private synchronized void toggle() {
    System.out.println((JokeServer.jokeMode.booleanValue()) ? "Entering Proverb Mode" : "Entering Joke Mode");
    JokeServer.jokeMode = !JokeServer.jokeMode.booleanValue();
  }

  private synchronized void shutDownJokeServer() {
    System.out.println("Shutdown Signal Received, GoodBye.");
    System.exit(0);
  }

  public void run() {
    try (BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()))){
      try {
        String name = input.readLine().trim();
        if (name.equals("shutdown")) {
          shutDownJokeServer();
        } else {
          toggle();
        }
      } catch (IOException e) {
        System.out.println("Server read error");
        e.printStackTrace();
      }
      socket.close();
    } catch (Exception e) {
      System.out.println(e);
    }
  }
}

class JokeWorker extends Thread {

  private static final String JOKE_ONE = "Joke one";
  private static final String JOKE_TWO = "Joke two";
  private static final String JOKE_THREE = "Joke three";
  private static final String JOKE_FOUR = "Joke four";

  private static final Collection<String> JOKES = new ArrayList<>(Arrays.asList(JOKE_ONE, JOKE_TWO, JOKE_THREE, JOKE_FOUR));

  private Socket sock;

  JokeWorker(Socket s) {
    sock = s;
  }

  public void run() {
    PrintStream outStream;
    BufferedReader in;
    try {
      in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
      outStream = new PrintStream(sock.getOutputStream());
      try {
        String name = in.readLine();
        tellJoke(name, outStream, JOKES);
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
    Random rand = new Random(System .currentTimeMillis()); // Could look into ThreadLocalRandom when converting to ASync
    int idx = rand.nextInt(4);
    ArrayList<String> jokeList = (ArrayList<String>) jokes;
    String joke = jokeList.get(idx);
    out.println(joke);
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
