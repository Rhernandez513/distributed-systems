/** File is: MyTelnetClient.java

2017-10-02, Clark Elliott

This is a quick and dirty reworking of the Inet program. Feel free to use and modify this code as you wish.

Simple mechanism for sending data to a webserver and retrieving the results.

To use:

> java MyTelnet condor.depaul.edu

GET /elliott/dog.html HTTP/1.1    [<-- Type this into the console window manually]
Host: condor.depaul.edu:80
[crlf]
[crlf]
stop

Should return something like:

HTTP/1.1 200 OK
Date: Mon, 02 Oct 2017 20:26:44 GMT
Server: Apache/2.2.3 (Red Hat)
Last-Modified: Wed, 07 Oct 2015 20:29:55 GMT
ETag: "8a1bfc-30-521899bff76c0"
Accept-Ranges: bytes
Content-Length: 48
Content-Type: text/plain
Connection: close

This is Elliott's dog file on condor. Good job!


----------------------------------------------------------------------*/

import java.io.*;
import java.net.*;

public class MyTelnetClient{

  private final static int PORT = 2540;

  public static void main (String args[]) {
    String serverName;
    if (args.length < 1) serverName = "localhost";
    else serverName = args[0];

    Socket sock;
    BufferedReader fromServer;
    PrintStream toServer;
    String textFromServer;

    System.out.println("Clark Elliott's MyTelnet Client, 1.0.\n");
    System.out.println("Using server: " + serverName + ", Port: " + PORT);
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    try {
      sock = new Socket(serverName, PORT);
      fromServer =
        new BufferedReader(new InputStreamReader(sock.getInputStream()));
      toServer = new PrintStream(sock.getOutputStream());

      String dataToSend;
      do {
        System.out.print
          ("Enter text to send to the server, <stop> to end: ");
        System.out.flush ();
        dataToSend = in.readLine ();
        if (dataToSend.indexOf("stop") < 0){
	  toServer.println(dataToSend);
	  toServer.flush();
	}
      } while (dataToSend.indexOf("stop") < 0);
      for (int i = 1; i <=20; i++){
        textFromServer = fromServer.readLine();
        if (textFromServer != null) System.out.println(textFromServer);
      }
      sock.close();
    } catch (IOException x) {x.printStackTrace ();}
  }
}
