import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class JokeServer {

  // Made these hard-coded values in the style of constants in Java to show intent and provide for readability

  private static final int Q_LENGTH = 6;
  private static final int CLIENT_PORT = 9001; // it's over 9000!
  private static final int ADMIN_PORT = 9002; // it's over 9000!

  private static final String JOKE_ONE = "Joke one";
  private static final String JOKE_TWO = "Joke two";
  private static final String JOKE_THREE = "Joke three";
  private static final String JOKE_FOUR = "Joke four";


  Collection<String> jokes = new ArrayList<>(Arrays.asList(JOKE_ONE, JOKE_TWO, JOKE_THREE, JOKE_FOUR));

  static volatile Boolean jokeMode = true; // if true = ProverbMode
  static volatile boolean keepAlive = true;

  public static void main(String a[]) throws IOException {
    System.out.println("This is a partial version!");

    Socket socket;
    ServerSocket servsock = new ServerSocket(CLIENT_PORT, Q_LENGTH);
    System.out.println( "Robert David's Joke Server 1.8 starting up, listening on client port " + CLIENT_PORT + "\n");
    System.out.println( "Robert David's Joke Server 1.8 starting up, listening on admin port " + ADMIN_PORT + "\n");
    while (keepAlive) {
      new ASyncAdminWorker(ADMIN_PORT).start();
//      socket = servsock.accept(); // this will block until a connection is accepted
////      // .start() will spawn a new thread and then call .run()
//      new JokeWorker(socket).start();
    }
  }
}

class ASyncAdminWorker extends Thread {
  private final int port;
  private final int BUFFER_SIZE = 1024;
  private final int TIMEOUT = 10; // in seconds
  private String hostname = "127.0.0.1"; // localhost default

  ASyncAdminWorker(int port) {
    this.port = port;
  }

  private synchronized void toggle() {
    System.out.println((JokeServer.jokeMode.booleanValue()) ? "Entering Proverb Mode" : "Entering Joke Mode");
    JokeServer.jokeMode = !JokeServer.jokeMode.booleanValue();
  }

  private synchronized void shutDownJokeServer() {
    System.out.println("Shutdown Signal Received, GoodBye.");
    JokeServer.keepAlive = false;
  }

  public void run() {
      try (AsynchronousServerSocketChannel server = AsynchronousServerSocketChannel.open()) {
        server.bind(new InetSocketAddress("127.0.0.1", port));
        Future<AsynchronousSocketChannel> acceptCon =
                server.accept();
        AsynchronousSocketChannel client = acceptCon.get(TIMEOUT, TimeUnit.SECONDS);
        if ((client!= null) && (client.isOpen())) {
          ByteBuffer byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);
          Future<Integer> valueReceived = client.read(byteBuffer);
          String messageFromAdminClient = new String(byteBuffer.array()).trim();
          valueReceived.get();
          if (messageFromAdminClient.equals("shutdown") || messageFromAdminClient.equals("shutdown\n")) {
            shutDownJokeServer();
          } else {
            toggle();
          }
//          byteBuffer.flip();
//          String str= "I'm fine. Thank you!";
//          Future<Integer> writeVal = client.write( ByteBuffer.wrap(str.getBytes()));
//          System.out.println("Writing back to client: " +str);
//          writeVal.get();
          byteBuffer.clear(); // maybe comment this out later
        }
        client.close();
      } catch (ExecutionException | IOException e) {
        // This is expected with client.close();
//      e.printStackTrace();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
}

class JokeWorker extends Thread {

  private static final String JOKE_ONE = "Joke one";
  private static final String JOKE_TWO = "Joke two";
  private static final String JOKE_THREE = "Joke three";
  private static final String JOKE_FOUR = "Joke four";

  Collection<String> jokes =
      new ArrayList<>(Arrays.asList(JOKE_ONE, JOKE_TWO, JOKE_THREE, JOKE_FOUR));
  private Socket sock;

  JokeWorker(Socket s) {
    sock = s;
  }

  public void run() {
    PrintStream outStream = null;
    BufferedReader in;
    try {
      in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
      outStream = new PrintStream(sock.getOutputStream());
      try {
        String name = in.readLine();
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
    Random rand = new Random( System .currentTimeMillis()); // Could look into ThreadLocalRandom when converting to ASync
    int idx = rand.nextInt(4);
    ArrayList<String> jokeList = (ArrayList<String>) jokes;
    String joke = jokeList.get(idx);
    out.println(joke);
  }

  private void parseMessage(String message) {}

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
