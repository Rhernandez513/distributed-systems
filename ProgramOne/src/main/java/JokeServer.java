import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class JokeServer {

  // Made these hard-coded values in the style of constants in Java to show intent and provide for
  // readability

  private static final int Q_LENGTH = 6; /* Number of requests for OpSys to queue */
  private static final int CLIENT_PORT = 4545;
  private static final int CLIENT_SECONDARY_PORT = 4546;
  private static final int ADMIN_PORT = 5050;
  private static final int ADMIN_SECONDARY_PORT = 5051;

  static volatile Flag jokeModeFlag = new Flag();

  public static void main(String a[]) throws IOException {

    boolean adminControlSwitch = true;

    System.out.println("This is a partial version!");

    System.out.println(
        "Robert David's Joke Server 1.8 starting up, listening on admin port " + ADMIN_PORT + "\n");
    // I tried my best to use a ASyncSocketChannel but couldn't get it to work using 2 ports, only 1
    // port
    // I will probably try again next time around.  I was having issue interrupting threads, and
    // running out of memory from creating
    // multiple threads.  I tried a few ways to limit the amount of threads spawning, but it didn't
    // alleviate the earlier issues
    Thread adminLoopThread =
        new Thread(
            () -> {
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
    System.out.println(
        "Robert David's Joke Server 1.8 starting up, listening on client port "
            + CLIENT_PORT
            + "\n");
    while (true) {
      // this will block until a connection is accepted and then spawn a new thread
      // .start() will spawn a new thread and then call .run()
      new JokeProverbWorker(servsock.accept()).start();
    }
  }
}

// Allegedly a thread-safe way to toggle an AtomicBoolean
// from The CERT Oracle Secure Coding Standard for Java
// https://books.google.de/books?id=FDun60sHwUgC&pg=PA314&lpg=PA314&dq=atomicboolean+toggle&source=bl&ots=_hZ-lOC6AW&sig=WnYDRP1GsUKsLJNxn6F3Ef5ty9Q&hl=de&sa=X&ved=0ahUKEwjIurG68ejXAhUO_aQKHbsSAvcQ6AEIVzAG#v=onepage&q=atomicboolean%20toggle&f=false
final class Flag {
  private AtomicBoolean flag = new AtomicBoolean(true);
  // Don't think this one needs to be synchronized because we are synchronizing at the above level
  // and the object is marked as volatile
  public void toggle() {
    boolean temp;
    do {
      temp = flag.get();
    } while (!flag.compareAndSet(temp, !temp));
  }

  public AtomicBoolean getFlag() {
    return flag;
  }
}

class AdminWorker extends Thread {

  private final Socket socket;

  AdminWorker(Socket socket) {
    this.socket = socket;
  }

  private synchronized void toggle() {
    System.out.println(
        (JokeServer.jokeModeFlag.getFlag().get()) ? "Entering Proverb Mode" : "Entering Joke Mode");
    JokeServer.jokeModeFlag.toggle();
  }

  private synchronized void shutDownJokeServer() {
    System.out.println("Shutdown Signal Received, GoodBye.");
    System.exit(0);
  }

  public void run() {
    try (BufferedReader input =
        new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
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

class JokeProverbWorker extends Thread {

  private static final String PROVERB_ONE = "Human behavior flows from three main sources: desire, emotion, and knowledge.  - Plato";
  private static final String PROVERB_TWO = "Success depends upon previous preparation, and without such preparation, there is sure to be failure.  - Confucius";
  private static final String PROVERB_THREE = "Let your plans be dark and impenetrable as night, and when you move, fall like a thunderbolt.  - Sun Tzu";
  private static final String PROVERB_FOUR = "It is the mark of an educated man to be able to entertain a thought without accepting it.  - Aristotle";

  private static final Collection<String> PROVERBS = new ArrayList<>(Arrays.asList(PROVERB_ONE, PROVERB_TWO, PROVERB_THREE, PROVERB_FOUR));

  private static final String JOKE_ONE = "What’s a pirates favorite letter?  You think it’s R but it be the C.";
  private static final String JOKE_TWO = "Time flies like an arrow, fruit flies like banana.";
  private static final String JOKE_THREE = "I have 3 kids and no money, why I can’t I have no kids and 3 money. - Homer Simpson";
  private static final String JOKE_FOUR = "I looked up my family tree and found out I was the sap.  – Rodney Dangerfield";

  private static final Collection<String> JOKES = new ArrayList<>(Arrays.asList(JOKE_ONE, JOKE_TWO, JOKE_THREE, JOKE_FOUR));

  private volatile AtomicReference<String> clientUserName = new AtomicReference<>();
  private Socket sock;

  JokeProverbWorker(Socket s) {
    sock = s;
  }

  public void run() {
    PrintStream outStream;
    BufferedReader in;
    try {
      in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
      outStream = new PrintStream(sock.getOutputStream());
      try {
        String clientResponse = in.readLine();
        if (JokeServer.jokeModeFlag.getFlag().get()) {
          List<boolean[]> state = parseClientNameAndState(clientResponse);
          // IDX of ZERO means Tell a Joke
          reciteProverbOrTellJoke(clientUserName.get(), outStream, JOKES, state.get(0));
        } else {
          // IDX of ONE means Recite a Proverb
          List<boolean[]> state = parseClientNameAndState(clientResponse);
          reciteProverbOrTellJoke(clientUserName.get(), outStream, PROVERBS, state.get(1));
        }
      } catch (IOException e) {
        System.out.println("Server read error");
        e.printStackTrace();
      }
      sock.close();
    } catch (IOException e) {
      System.out.println(e);
    }
  }

  private static boolean[] deSerializeState(String state) {
    boolean[] stateAsBools = new boolean[4];
    String[] content = state.trim().split(",");

    for(int i = 0; i < stateAsBools.length; ++i) {
      if (content[i].equals("true")) {
        stateAsBools[i] = true;
      } else {
        stateAsBools[i] = false;
      }
    }
    return stateAsBools;
  }

  private List<boolean[]> parseClientNameAndState(String messageToParse) {
    String[] content = messageToParse.split(";");
    clientUserName.set(content[0].trim());

    boolean[] jokeState = deSerializeState(content[1].trim());
    boolean[] proverbState = deSerializeState(content[2].trim());

    return new ArrayList<>(Arrays.asList(jokeState, proverbState));
  }

  // This will take a cookie from an individual client
  // and determine which indexes are false AKA have not been used yet
  private static Set<Integer> analyzeState(boolean[] state) {
    Set<Integer> workableIndexes = new HashSet<>();

    for (int i = 0; i < state.length; ++i) {
      if (state[i] == false) {
        workableIndexes.add(i);
      }
    }

    if (workableIndexes.size() == 0) {
      // All of the jokes or proverbs have been used up already
      return null;
    }
    return workableIndexes;
  }

  private static void reciteProverbOrTellJoke(String name, PrintStream out, Collection<String> proverbs, boolean[] state) {

    Random rand = new Random( System .currentTimeMillis()); // Could look into ThreadLocalRandom when converting to ASync

    ArrayList<String> prefixes;

    if(JokeServer.jokeModeFlag.getFlag().get()) {
      // We are in Joke Mode
      prefixes = new ArrayList<>(Arrays.asList("JA", "JB", "JC", "JD"));
    } else {
      prefixes = new ArrayList<>(Arrays.asList("PA", "PB", "PC", "PD"));
    }

    // We start with getting any random number
    int idx = rand.nextInt(4);

    // We want to pinpoint a potential index of the state array that hasn't been used yet
    Set<Integer> indexesToUse = analyzeState(state);

    // All of the potential proverbs have been used up, lets reset the state
    if (indexesToUse == null) {
      state = new boolean[] {false, false, false, false};
    } else {
      // If they haven't been used up, lets determine what joke or proverb is good to use
      while (!indexesToUse.contains(idx)) {
        idx = rand.nextInt(4);
      }
    }

    ArrayList<String> proverbList = (ArrayList<String>) proverbs;
    String proverb = proverbList.get(idx);

    // Here we set the reported proverb index to true so it wont be repeated
    state[idx] = true;

    // Here we check if we have seen all of the proverbs or jokes
    boolean all_done_flag = true;
    for (boolean b : state) {
      if (b == false) {
        all_done_flag = false;
      }
    }
    if (all_done_flag) {
      if (JokeServer.jokeModeFlag.getFlag().get()) {
        System.out.println("JOKE CYCLE COMPLETED");
      } else {
        System.out.println("PROVERB CYCLE COMPLETED");
      }
    }

    // Send the message w/ updated state back to the client
    String stateUpdateMessage = "STATE: ";
    for(boolean a : state) {
      stateUpdateMessage += a + ", ";
    }

    // Indicate to the client what mode the server is in
    String modeMessage = "MODE: ";
    if (JokeServer.jokeModeFlag.getFlag().get()) {
      modeMessage += "JOKE";
    } else {
      modeMessage += "PROVERB";
    }
    out.println(modeMessage);

    // Update the clients state, the client will determine what state-mode it needs to update
    out.println(stateUpdateMessage.substring(0, stateUpdateMessage.length() - 2));

    // Send the actual joke or proverb
    out.println(prefixes.get(idx) + " " + name + " " + proverb);
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
