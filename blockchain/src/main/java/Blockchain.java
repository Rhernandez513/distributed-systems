/* 2018-01-14:
bc.java for BlockChain
Dr. Clark Elliott for CSC435

This is some quick sample code giving a simple framework for coordinating multiple processes in a blockchain group.

INSTRUCTIONS:

Set the numProceses class variable (e.g., 1,2,3), and use a batch file to match

AllStart.bat:

REM for three procesess:
start java bc 0
start java bc 1
java bc 2

You might want to start with just one process to see how it works.

Thanks: http://www.javacodex.com/Concurrency/PriorityBlockingQueue-Example

Notes to CDE:
Optional: send public key as Base64 XML along with a signed string.
Verfy the signature with public key that has been restored.

*/

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

// Would normally keep a process block for each process in the multicast group:
/* class ProcessBlock{
  int processID;
  PublicKey pubKey;
  int port;
  String IPAddress;
  } */

class Ports{
  public static int KeyServerPortBase = 4710;
  public static int UnverifiedBlockServerPortBase = 4820;
  public static int BlockchainServerPortBase = 4930;

  public static int KeyServerPort;
  public static int UnverifiedBlockServerPort;
  public static int BlockchainServerPort;

  public void setPorts(){
    KeyServerPort = KeyServerPortBase + (Blockchain.PID);
    UnverifiedBlockServerPort = UnverifiedBlockServerPortBase + (Blockchain.PID);
    BlockchainServerPort = BlockchainServerPortBase + (Blockchain.PID);
  }
}

class PublicKeyWorker extends Thread {
  Socket sock;

  PublicKeyWorker (Socket s) {sock = s;}

  public void run() {
    try (BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()))) {
      String data = in.readLine();
      // TODO perhaps do something with these Public Keys
      System.out.println("Got key: " + data);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}

class PublicKeyServer implements Runnable {
  //public ProcessBlock[] PBlock = new ProcessBlock[3]; // One block to store info for each process.

  public void run() {
    Socket sock;
    System.out.println("Starting Key Server input thread using: " + Ports.KeyServerPort);
    try {
      ServerSocket servsock = new ServerSocket(Ports.KeyServerPort, Blockchain.Q_LEN);
      while (true) {
        sock = servsock.accept();
        new PublicKeyWorker (sock).start();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}

class UnverifiedBlockServer implements Runnable {
  BlockingQueue<String> queue;
  UnverifiedBlockServer(BlockingQueue<String> queue){
    this.queue = queue;
  }

  /* Inner class to share priority queue. We are going to place the unverified blocks into this queue in the order we get
     them, but they will be retrieved by a consumer process sorted by blockID. */

  class UnverifiedBlockWorker extends Thread {
    Socket sock;
    UnverifiedBlockWorker (Socket s) {sock = s;}
    public void run(){
      try (BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()))){
        String data = in.readLine ();
        // TODO Double check that this Worker doesn't have additional scope in the assignment
        System.out.println("Put in priority queue: " + data + "\n");
        queue.put(data);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public void run() {
    Socket sock;
    System.out.println("Starting the Unverified Block Server input thread using " + Ports.UnverifiedBlockServerPort);
    try {
      ServerSocket servsock = new ServerSocket(Ports.UnverifiedBlockServerPort, Blockchain.Q_LEN);
      while (true) {
        sock = servsock.accept(); // Got a new unverified block
        new UnverifiedBlockWorker(sock).start(); // So start a thread to process it.
      }
    } catch (IOException e) {
      System.out.println(e);
    }
  }
}

/* We have received unverified blocks into a thread-safe concurrent access queue. Just for the example, we retrieve them
in order according to their blockID. Normally we would retrieve the block with the lowest time stamp first, or? This
is just an example of how to implement such a queue. It must be concurrent safe because two or more threads modify it
"at once," (multiple worker threads to add to the queue, and consumer thread to remove from it).*/

class UnverifiedBlockConsumer implements Runnable {
  BlockingQueue<String> queue;
//  int PID;
  UnverifiedBlockConsumer(BlockingQueue<String> queue){ this.queue = queue; }

  public void run(){
    String data;
    PrintStream toServer;
    Socket sock;
//    String newblockchain;
    String fakeVerifiedBlock;

    System.out.println("Starting the Unverified Block Priority Queue Consumer thread.\n");
    try{
      while(true){ // Consume from the incoming queue. Do the work to verify. Mulitcast new blockchain
        // TODO notice here the data object is the unverifiedBlock
        data = queue.take(); // Will blocked-wait on empty queue
        System.out.println("Consumer got unverified: " + data);

        // TODO Ordinarily we would do real work here, based on the incoming data.
        int j; // Here we fake doing some work (That is, here we could cheat, so not ACTUAL work...)
        for(int i=0; i< 100; i++){ // put a limit on the fake work for this example
          j = ThreadLocalRandom.current().nextInt(0,10);
          try { Thread.sleep(500); } catch(Exception e) { e.printStackTrace(); }
          if (j < 3) break; // <- how hard our fake work is; about 1.5 seconds.
        }

        /* With duplicate blocks that have been verified by different processes ordinarily we would keep only the one with
        the lowest verification timestamp. For the example we use a crude filter, which also may let some duplicates through */
        // TODO create a true filter for the lowest timestamp
        if(Blockchain.blockchain.indexOf(data.substring(1, 9)) < 0) { // Crude, but excludes most duplicates.
          fakeVerifiedBlock = "[" + data + " verified by P" + Blockchain.PID + " at time "
            + ThreadLocalRandom.current().nextInt(100,1000) + "]\n";
          System.out.println(fakeVerifiedBlock);
          String tempblockchain = fakeVerifiedBlock + Blockchain.blockchain; // add the verified block to the chain
          for(int i = 0; i < Blockchain.numProcesses; i++){ // send to each process in group, including us:
            sock = new Socket(Blockchain.serverName, Ports.BlockchainServerPortBase + (i));
            toServer = new PrintStream(sock.getOutputStream());

            // make the multicast
            toServer.println(tempblockchain); toServer.flush();

            sock.close();
          }
        }
        Thread.sleep(1500); // For the example, wait for our blockchain to be updated before processing a new block
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

// Incoming proposed replacement blockchains. Compare to existing. Replace if winner:
class BlockchainWorker extends Thread {
  Socket sock;
  BlockchainWorker (Socket s) {sock = s;}
  public void run(){
    try{
      BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
      String data = "";
      String data2;
      while((data2 = in.readLine()) != null){
        data = data + data2;
      }
      // TODO Check here for "winner"
      Blockchain.blockchain = data; // Would normally have to check first for winner before replacing.
      System.out.println("         --NEW BLOCKCHAIN--\n" + Blockchain.blockchain + "\n\n");
      sock.close();
    } catch (IOException x){x.printStackTrace();}
  }
}

class BlockchainServer implements Runnable {
  public void run(){
    Socket sock;
    System.out.println("Starting the blockchain server input thread using " + Ports.BlockchainServerPort);
    try{
      ServerSocket servsock = new ServerSocket(Ports.BlockchainServerPort, Blockchain.Q_LEN);
      while (true) {
        sock = servsock.accept();
        new BlockchainWorker (sock).start();
      }
    }catch (IOException ioe) {System.out.println(ioe);}
  }
}

// Class bc for BlockChain
public class Blockchain {

  public final static int Q_LEN = 6;

  static String serverName = "localhost";
  static String blockchain = "[First block]";
  final static int numProcesses = 3; // Set this to match your batch execution file that starts N processes with args 0,1,2,...N
  static int PID;

  // Multicast some data to each of the processes.
  public void MultiSend (){
    try {
      String fakeKey = ("FakeKeyProcess: " + Blockchain.PID);
      broadcast(Ports.KeyServerPortBase, fakeKey);

      Thread.sleep(1000); // wait for keys to settle, normally would wait for an ack

      //Fancy arithmetic is just to generate identifiable blockIDs out of numerical sort order:
      String fakeBlockA = "(Block#" + ((Blockchain.PID+1)*10)+4 + " from P"+ Blockchain.PID + ")";
      String fakeBlockB = "(Block#" + ((Blockchain.PID+1)*10)+3 + " from P"+ Blockchain.PID + ")";

      broadcast(Ports.UnverifiedBlockServerPortBase, fakeBlockA);
      broadcast(Ports.UnverifiedBlockServerPortBase, fakeBlockB);

    } catch (InterruptedException | IOException e) {
      e.printStackTrace();
    }
  }

  private void broadcast(int port, String payload) throws IOException {
    Socket sock;
    PrintStream toServer;
    for(int i=0; i< numProcesses; i++){// Send a sample unverified block to each server
      sock = new Socket(serverName, port + (i));
      toServer = new PrintStream(sock.getOutputStream());
      toServer.println(payload);
      toServer.flush();
      sock.close();
    }
  }

  public static void main(String args[]){

    PID = (args.length < 1) ? 0 : Integer.parseInt(args[0]); // Process ID
    System.out.println("Robert's BlockFramework control-c to quit.\n");
    System.out.println("Using processID " + PID + "\n");

    final BlockingQueue<String> queue = new PriorityBlockingQueue<>(); // Concurrent queue for unverified blocks
    new Ports().setPorts(); // Establish OUR port number scheme, based on PID

    new Thread(new PublicKeyServer()).start(); // New thread to process incoming public keys
    new Thread(new UnverifiedBlockServer(queue)).start(); // New thread to process incoming unverified blocks
    new Thread(new BlockchainServer()).start(); // New thread to process incoming new blockchains
    try{Thread.sleep(1000);}catch(Exception e){} // Wait for servers to start.
    new Blockchain().MultiSend(); // Multicast some new unverified blocks out to all servers as data
    try{Thread.sleep(1000);}catch(Exception e){} // Wait for multicast to fill incoming queue for our example.

    new Thread(new UnverifiedBlockConsumer(queue)).start(); // Start consuming the queued-up unverified blocks
  }
}
