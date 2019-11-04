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

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

// Would normally keep a process block for each process in the multicast group:
/* class ProcessBlock{
  int processID;
  PublicKey pubKey;
  int port;
  String IPAddress;
  } */

class Ports {
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
  BlockingQueue<BlockRecord> queue;
  UnverifiedBlockServer(BlockingQueue<BlockRecord> queue){
    this.queue = queue;
  }

  /* Inner class to share priority queue. We are going to place the unverified blocks into this queue in the order we get
     them, but they will be retrieved by a consumer process sorted by blockID. */

  class UnverifiedBlockWorker extends Thread {
    Socket sock;
    UnverifiedBlockWorker (Socket s) {sock = s;}

    private BlockRecord unMarshallXMLBlock(String XMLBlock) {

      XMLBlock.replace("\n", "");

      // From: https://www.intertech.com/Blog/jaxb-tutorial-how-to-marshal-and-unmarshal-xml/
      // Credit to Joseph Varilla from class for the link
      try {
        JAXBContext jaxbContext = JAXBContext.newInstance(BlockRecord.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

        // Unmarshallers are not thread-safe.  Create a new one every time.
        XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(XMLBlock));

        JAXBElement<BlockRecord> result = jaxbUnmarshaller.unmarshal(reader, BlockRecord.class);
        return result.getValue();

      } catch (Exception e) {
        e.printStackTrace();
      }
      return null;
    }

    public void run(){
      try (BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()))){
        String data = in.readLine ();
        // 0 - 6 contant "PID: X" where X is the pnum, perhaps useful later
        data = data.substring(6);
        data = data.replace("--linebreak--", "\n");

        BlockRecord blockRecord = unMarshallXMLBlock(data);

        System.out.println("Put in priority queue:\n" + data + "\n");
        queue.put(blockRecord);
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
  BlockingQueue<BlockRecord> queue;
  int PID;
  UnverifiedBlockConsumer(BlockingQueue<BlockRecord> queue, int PID){
    this.queue = queue;
    this.PID = PID;
  }

  public void run(){
    BlockRecord data;
    PrintStream toServer;
    Socket sock;
    String newblockchain = Blockchain.blockchain;
    String fakeVerifiedBlock;

    System.out.println("Starting the Unverified Block Priority Queue Consumer thread.\n");
    try{
      while(true){ // Consume from the incoming queue. Do the work to verify. Mulitcast new blockchain
        data = queue.take(); // Will blocked-wait on empty queue
        System.out.println("Consumer got unverified Block:\n" + data + "\n");


        BlockRecord verifiedBlock = WorkB.verifyBlock(data, this.PID);
        verifiedBlock.setAVerificationProcessID(Integer.toString(this.PID));

        // !! At this point a block should be "solved" !! //

        // The blow logic is primarily concerned with determining if the "solved" block can be appened to the end of the
        // block chain and then rebroadcasting it to the rest of the chain

        /* With duplicate blocks that have been verified by different processes ordinarily we would keep only the one with
        the lowest verification timestamp. For the example we use a crude filter, which also may let some duplicates through */
//        if(Blockchain.blockchain.indexOf(data.substring(1, 9)) < 0) { // Crude, but excludes most duplicates.
//          fakeVerifiedBlock = "[" + data + " verified by P" + Blockchain.PID + " at time " + Instant.now() + "]\n";
//          System.out.println(fakeVerifiedBlock);
          String tempblockchain = verifiedBlock.toString().replace(" ", "") + Blockchain.blockchain; // add the verified block to the chain
          for(int i = 0; i < Blockchain.numProcesses; i++){ // send to each process in group, including us:
            sock = new Socket(Blockchain.serverName, Ports.BlockchainServerPortBase + (i));
            toServer = new PrintStream(sock.getOutputStream());

            // make the multicast
            toServer.println(tempblockchain); toServer.flush();

            sock.close();
          }
//        }
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

// Solution class for BlockChain assignment
public class Blockchain {

  public final static int Q_LEN = 6;

  static String serverName = "localhost";
  static String blockchain = "[First block]";

  // TODO don't forget to change this back to 3 when not DEBUGGING
  final static int numProcesses = 1; // Set this to match your batch execution file that starts N processes with args 0,1,2,...N
  static int PID;

  // Multicast some data to each of the processes.
  public void MultiSend (){
    try {
      String fakeKey = ("FakeKeyProcess: " + Blockchain.PID);
      broadcast(Ports.KeyServerPort, fakeKey);

      Thread.sleep(1000); // wait for keys to settle, normally would wait for an ack

      broadcast(Ports.BlockchainServerPort, blockchain);

      String [] args = { String.valueOf(PID) } ;
      String XMLBlock = BlockInput.getXMLBlock(args);

      List<String> blocks =  stripBlockLedgerHeader(XMLBlock);

      for (String block : blocks) {
        block = block.replace("\n", "--linebreak--");
        broadcast(Ports.UnverifiedBlockServerPort, "PID: " + Blockchain.PID + block);
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private List<String> stripBlockLedgerHeader(String xmlBlock) {

    String[] blocks = xmlBlock.split("\n\n");
    blocks[0] = blocks[0].split("<BlockLedger>")[1].trim();
    blocks[blocks.length - 1] = blocks[blocks.length - 1].split("</BlockLedger>")[0].trim();

    return Arrays.asList(blocks);
  }

  public static void broadcast(int port, String payload) throws IOException {
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

    final BlockingQueue<BlockRecord> queue = new PriorityBlockingQueue<>(); // Concurrent queue for unverified blocks
    new Ports().setPorts(); // Establish OUR port number scheme, based on PID

    // New thread to process incoming public keys
    new Thread(new PublicKeyServer()).start();
    // New thread to process incoming unverified blocks
    new Thread(new UnverifiedBlockServer(queue)).start();
    // New thread to process incoming new blockchains
    new Thread(new BlockchainServer()).start();
    // Wait for servers to start.
    try{Thread.sleep(1000);}catch(Exception e){}
    // Multicast some new unverified blocks out to all servers as data
    new Blockchain().MultiSend();
    // Wait for multicast to fill incoming queue for our example.
    try{Thread.sleep(1000);}catch(Exception e){}

    // Start consuming the queued-up unverified blocks
    new Thread(new UnverifiedBlockConsumer(queue, PID)).start();
  }
}

// BEGIN BlockInputE //

@XmlRootElement
class BlockRecord implements Comparable {
  /* Examples of block fields: */
  int BlockNum;
  String BlockData;
  String Timestamp;
  String SHA256String;
  String SignedSHA256;
  String BlockID;
  String VerificationProcessID;
  String CreatingProcess;
  String PreviousHash;
  String Fname;
  String Lname;
  String SSNum;
  String DOB;
  String Diag;
  String Treat;
  String Rx;

  /* Examples of accessors for the BlockRecord fields. Note that the XML tools sort the fields alphabetically
     by name of accessors, so A=header, F=Indentification, G=Medical: */

  public String getABlockData() {return this.BlockData;}
  @XmlElement
  public void setABlockData(String blockData){this.BlockData = blockData;}

  public int getABlockNum() {return this.BlockNum;}
  @XmlElement
  public void setABlockNum(int blockNum){this.BlockNum = blockNum;}

  public String getATimestamp() {return Timestamp;}
  @XmlElement
  public void setATimestamp(String TS){this.Timestamp = TS;}

  public String getASHA256String() {return SHA256String;}
  @XmlElement
  public void setASHA256String(String SH){this.SHA256String = SH;}

  public String getASignedSHA256() {return SignedSHA256;}
  @XmlElement
  public void setASignedSHA256(String SH){this.SignedSHA256 = SH;}

  public String getACreatingProcess() {return CreatingProcess;}
  @XmlElement
  public void setACreatingProcess(String CP){this.CreatingProcess = CP;}

  public String getAVerificationProcessID() {return VerificationProcessID;}
  @XmlElement
  public void setAVerificationProcessID(String VID){this.VerificationProcessID = VID;}

  public String getABlockID() {return BlockID;}
  @XmlElement
  public void setABlockID(String BID){this.BlockID = BID;}

  public String getFSSNum() {return SSNum;}
  @XmlElement
  public void setFSSNum(String SS){this.SSNum = SS;}

  public String getFFname() {return Fname;}
  @XmlElement
  public void setFFname(String FN){this.Fname = FN;}

  public String getFLname() {return Lname;}
  @XmlElement
  public void setFLname(String LN){this.Lname = LN;}

  public String getFDOB() {return DOB;}
  @XmlElement
  public void setFDOB(String DOB){this.DOB = DOB;}

  public String getGDiag() {return Diag;}
  @XmlElement
  public void setGDiag(String D){this.Diag = D;}

  public String getGTreat() {return Treat;}
  @XmlElement
  public void setGTreat(String D){this.Treat = D;}

  public String getGRx() {return Rx;}
  @XmlElement
  public void setGRx(String D){this.Rx = D;}

  @Override
  public String toString() {
    try {
      JAXBContext jaxbContext = JAXBContext.newInstance(BlockRecord.class);
      Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
      StringWriter sw = new StringWriter();

      jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
      jaxbMarshaller.marshal(this, sw);

      String result = sw.toString();

      String XMLHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";
      String cleanBlock = result.replace(XMLHeader, "");

      return cleanBlock.trim();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public int compareTo(Object o) {

    Instant otherInstant;

    BlockRecord other = (BlockRecord) o;

    otherInstant = Instant.parse(other.getATimestamp());
    Instant thisInstant = Instant.parse(this.getATimestamp());

    // https://docs.oracle.com/javase/8/docs/api/java/lang/Comparable.html
    // Returns a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object.
    return thisInstant.compareTo(otherInstant);
  }
}

class BlockInput {

  private static String FILENAME;

  /* Token indexes for input: */
  private static final int iFNAME = 0;
  private static final int iLNAME = 1;
  private static final int iDOB = 2;
  private static final int iSSNUM = 3;
  private static final int iDIAG = 4;
  private static final int iTREAT = 5;
  private static final int iRX = 6;


  public static String getXMLBlock(String[] args) throws Exception {

    String result;

    /* CDE: Process numbers and port numbers to be used: */
    int pnum;
    int UnverifiedBlockPort;
    int BlockChainPort;

    /* CDE If you want to trigger bragging rights functionality... */
    if (args.length > 1) System.out.println("Special functionality is present \n");

    if (args.length < 1) pnum = 0;
    else if (args[0].equals("0")) pnum = 0;
    else if (args[0].equals("1")) pnum = 1;
    else if (args[0].equals("2")) pnum = 2;
    else pnum = 0; /* Default for badly formed argument */

    UnverifiedBlockPort = 4820 + pnum;
    BlockChainPort = 4930 + pnum;

    System.out.println("Process number: " + pnum + " Ports: " + UnverifiedBlockPort + " " +
            BlockChainPort + "\n");

    switch(pnum) {
      case 1: FILENAME = "BlockInput1.txt"; break;
      case 2: FILENAME = "BlockInput2.txt"; break;
      default: FILENAME= "BlockInput0.txt"; break;
    }

    System.out.println("Using input file: " + FILENAME);

    try (BufferedReader br = new BufferedReader(new FileReader(FILENAME))) {
      String[] tokens = new String[10];
      String stringXML;
      String InputLineStr;
      String suuid;
      UUID idA;

      BlockRecord[] blockArray = new BlockRecord[20];

      JAXBContext jaxbContext = JAXBContext.newInstance(BlockRecord.class);
      Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
      StringWriter sw = new StringWriter();

      // CDE Make the output pretty printed:
      jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

      int n = 0;

      while ((InputLineStr = br.readLine()) != null) {
        blockArray[n] = new BlockRecord();

        String TS = Instant.now().toString();
        blockArray[n].setATimestamp(TS);

        blockArray[n].setASHA256String("");

        // We will "sign" by generating a new SHA256 based on the on the process number plus the original SHA256
        // Getting a real private key setup takes some more time to code but signing works the same way
        blockArray[n].setASignedSHA256("");

        /* CDE: Generate a unique blockID. This would also be signed by creating process: */
        idA = UUID.randomUUID();
        suuid = UUID.randomUUID().toString();
        blockArray[n].setABlockID(suuid);
        blockArray[n].setACreatingProcess("Process" + pnum);
        // TODO check assignment desc for this one
        blockArray[n].setAVerificationProcessID("To be set later...");
        /* CDE put the file data into the block record: */
        tokens = InputLineStr.split(" +"); // Tokenize the input
        blockArray[n].setFSSNum(tokens[iSSNUM]);
        blockArray[n].setFFname(tokens[iFNAME]);
        blockArray[n].setFLname(tokens[iLNAME]);
        blockArray[n].setFDOB(tokens[iDOB]);
        blockArray[n].setGDiag(tokens[iDIAG]);
        blockArray[n].setGTreat(tokens[iTREAT]);
        blockArray[n].setGRx(tokens[iRX]);
        n++;
      }
      System.out.println(n + " records read.");
      System.out.println("Names from input:");
      for(int i=0; i < n; i++){
        System.out.println("  " + blockArray[i].getFFname() + " " +
                blockArray[i].getFLname());
      }
      System.out.println("\n");

      stringXML = sw.toString();
      for(int i=0; i < n; i++){
        jaxbMarshaller.marshal(blockArray[i], sw);
      }
      String fullBlock = sw.toString();
      String XMLHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";
      String cleanBlock = fullBlock.replace(XMLHeader, "");
      // Show the string of concatenated, individual XML blocks:
      String XMLBlock = XMLHeader + "\n<BlockLedger>" + cleanBlock + "</BlockLedger>";
      return XMLBlock;
    } catch (IOException e) {e.printStackTrace();}
    return null;
  }
}

class WorkB {

  private static final String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
  static String someText = "one two three";
  static String randomString;

  // Gets a new random AlphaNumeric seed string
  public static String randomAlphaNumeric(int count) {
    StringBuilder builder = new StringBuilder();
    while (count-- != 0) {
      int character = (int)(Math.random()*ALPHA_NUMERIC_STRING.length());
      builder.append(ALPHA_NUMERIC_STRING.charAt(character));
    }
    return builder.toString();
  }

  // If using a real key I would base it on this:
  // https://www.quickprogrammingtips.com/java/how-to-create-sha256-rsa-signature-using-java.html
  // We will "sign" by generating a new SHA256 based on the on the process number ("private key") plus the original SHA256
  // Getting a real private key setup takes some more time to code but signing works the same way
  private static String signSHA256(String input, String privateKey) throws Exception {

    String hashInput = privateKey + input;

    MessageDigest MD = MessageDigest.getInstance("SHA-256");
    byte[] bytesHash = MD.digest(hashInput.getBytes("UTF-8"));

    // Turn into a string of hex values
    String SHAString = DatatypeConverter.printHexBinary(bytesHash);

    return SHAString;
  }


  public static BlockRecord verifyBlock(BlockRecord inputBlock, int PID) throws Exception {
    String inputBlockWithBlockData;  // Random seed string concatenated with the existing data
    String hashStringOut; // Will contain the new SHA256 string converted to HEX and printable.
    int workNumber;

    try {
      for(int i=1; i<20; i++) { // Limit how long we try for this example.
        randomString = randomAlphaNumeric(8);

        inputBlock.setABlockData(randomString);

        // Concatenate with our input string (which represents Blockdata)
        inputBlockWithBlockData = inputBlock.toString().replace("\n", "").replace(" ", "");

        // Get the hash value
        MessageDigest MD = MessageDigest.getInstance("SHA-256");
        byte[] bytesHash = MD.digest(inputBlockWithBlockData.getBytes("UTF-8"));

        // Turn into a string of hex values
        hashStringOut = DatatypeConverter.printHexBinary(bytesHash);
        System.out.println("Hash is: " + hashStringOut);

        // Between 0000 (0) and FFFF (65535)
        workNumber = Integer.parseInt(hashStringOut.substring(0,4),16);

        System.out.println("First 16 bits in Hex and Decimal: " + hashStringOut.substring(0,4) +" and " + workNumber);

        if (!(workNumber < 20000)) {  // lower number = more work.
          System.out.format("%d is not less than 20,000 so we did not solve the puzzle\n\n", workNumber);
        }
        if (workNumber < 20000) {
          System.out.format("%d IS less than 20,000 so puzzle solved!\n", workNumber);
          System.out.println("The seed (puzzle answer) was: " + randomString);
          inputBlock.setASHA256String(hashStringOut);
          inputBlock.setASignedSHA256(signSHA256(inputBlock.getASHA256String(), String.valueOf(PID)));
          return inputBlock;
        }
        // TODO Here is where you would periodically check to see if the blockchain has been updated
        // ...if so, then abandon this verification effort and start over.
        // Here is where you will sleep if you want to extend the time up to a second or two.
      }
    } catch(Exception ex) {
      ex.printStackTrace();
    }
    return null;
  }
}
