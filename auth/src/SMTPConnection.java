import java.net.*;
import java.io.*;
import java.util.*;

/**
 * Open an SMTP connection to a remote machine and send one mail.
 *
 */
public class SMTPConnection {
    /* The socket to the server */
    private Socket connection;

    /* Streams for reading and writing the socket */
    private BufferedReader fromServer;
    private DataOutputStream toServer;

    private static final int SMTP_PORT = 25;
    private static final String CRLF = "\r\n";

    /* Are we connected? Used in close() to determine what to do. */
    private boolean isConnected = false;

    /* Create an SMTPConnection object. Create the socket and the 
       associated streams. Initialize SMTP connection. */
    public SMTPConnection(Envelope envelope) throws IOException {
        connection = new Socket(envelope.SourceAddr, SMTP_PORT);
        fromServer = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        toServer =  new DataOutputStream(connection.getOutputStream());

	String reply = fromServer.readLine();
    if(parseReply(reply) != 220) {
        throw new IOException("Error in message.");
    }

	/* SMTP handshake. We need the name of the local machine.
	   Send the appropriate SMTP handshake command. */
	String localhost = InetAddress.getLocalHost().getHostName();

	sendCommand("HELO " + envelope.SourceHost, 250);
	sendCommand("AUTH LOGIN", 334);
	sendCommand(Base64.getEncoder().encodeToString(envelope.SenderName.getBytes()), 334);
	sendCommand(Base64.getEncoder().encodeToString(envelope.Password.getBytes()), 235);

	isConnected = true;
    }

    /* Send the message. Write the correct SMTP-commands in the
       correct order. No checking for errors, just throw them to the
       caller. */
    public void send(Envelope envelope) throws IOException {
        sendCommand("MAIL FROM: <" + envelope.Sender + ">", 250);
        sendCommand("RCPT TO: <" + envelope.Recipient + ">", 250);
        sendCommand("DATA", 354);
        sendCommand(envelope.Message.toString() + CRLF + ".", 250);
    }

    /* Close the connection. First, terminate on SMTP level, then
       close the socket. */
    public void close() {
	isConnected = false;
	try {
	    sendCommand("QUIT", 221);
	    connection.close();
	} catch (IOException e) {
	    System.out.println("Unable to close connection: " + e);
	    isConnected = true;
	}
    }

    /* Send an SMTP command to the server. Check that the reply code
       is what it is supposed to be according to RFC 821. */
    private void sendCommand(String command, int rc) throws IOException {
	toServer.writeBytes(command + CRLF);
	String reply = fromServer.readLine();
	if(parseReply(reply) != rc) {
	    throw new IOException("Wrong reply: " + reply);
    }
    }

    /* Parse the reply line from the server. Returns the reply  code. */
    private int parseReply(String reply) {
        StringTokenizer parser = new StringTokenizer(reply);
        String replyCode = parser.nextToken();
        return new Integer(replyCode);
    }

    /* Destructor. Closes the connection if something bad happens. */
    protected void finalize() throws Throwable {
	if(isConnected) {
	    close();
	}
	super.finalize();
    }
}


