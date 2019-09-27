import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class JokeServerAdmin {
  // We define a constant in the java style
  private static final int PORT = 9002; // it's over 9000!

  void toggleServer(String ipAddress) {

  }

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
        if (machineName.equals("shutdown") || machineName.equals("shutdown\n")) {
          ASyncShutdown();
        }
        if (machineName.indexOf("quit") < 0) {
          ASyncToggle();
        }
      } while (machineName.indexOf("quit") < 0); // Loop until the user wants to exit
      System.out.println("Cancelled by user request.");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  static void ASyncShutdown() {
    try {
      AsynchronousSocketChannel client = AsynchronousSocketChannel.open();
      Future<Void> result = client.connect( new InetSocketAddress("127.0.0.1", PORT));
      result.get();
//      String str= "Hello! How are you?";
      String str= "shutdown";
      ByteBuffer buffer = ByteBuffer.wrap(str.getBytes());
      Future<Integer> writeval = client.write(buffer);
      System.out.println("Writing to server: "+str);
      writeval.get();
      buffer.flip();
      Future<Integer> readval = client.read(buffer);
      System.out.println("Received from server: " +new String(buffer.array()).trim());
      readval.get();
      buffer.clear();
      client.close();
    }
    catch (ExecutionException | IOException e) {
      // This is expected with client.close();
//      e.printStackTrace();
    }
    catch (InterruptedException e) {
      System.out.println("Disconnected from the server.");
    }
  }

  static void ASyncToggle() {
    try {
      AsynchronousSocketChannel client = AsynchronousSocketChannel.open();
      Future<Void> result = client.connect( new InetSocketAddress("127.0.0.1", PORT));
      result.get();
      String str= "Hello! How are you?";
      ByteBuffer buffer = ByteBuffer.wrap(str.getBytes());
      Future<Integer> writeval = client.write(buffer);
      System.out.println("Writing to server: "+str);
      writeval.get();
      buffer.flip();
      Future<Integer> readval = client.read(buffer);
      System.out.println("Received from server: "
              +new String(buffer.array()).trim());
      readval.get();
      buffer.clear();
      client.close();
    }
    catch (ExecutionException | IOException e) {
      // This is expected with client.close();
//      e.printStackTrace();
    }
    catch (InterruptedException e) {
      System.out.println("Disconnected from the server.");
    }
  }
}
