import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class MyListener {

  private static final int Q_LENGTH = 6; /* Number of requests for OpSys to queue */
  private static final int PORT = 2540;

  public static void main(String[] args) {
    try {

      ServerSocket servsock = new ServerSocket(PORT, Q_LENGTH);
      while (true) {
        // Will block on .accept()
        new Thread(new ListenerWorker(servsock.accept())).start();
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

class ListenerWorker implements Runnable {
  private Socket socket;
  ListenerWorker (Socket socket) {
    this.socket = socket;
  }

  public void run() {
    try (BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
      String line;
      do {
        line = input.readLine();
        if (line == null) {
          break;
        }
        System.out.println(line.trim());
      } while (line != null);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

