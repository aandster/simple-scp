// ChatServer
// Author: Jordan Maddock
// Date: 31/08/2018
// This is the server for the simple-scp project.

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class ChatServer {

    public static void main(String[] args) throws IOException {
        String LOCAL_HOSTNAME = null, REMOTE_HOSTNAME, WELCOME_MSG;
        int LOCAL_PORT, REMOTE_PORT;

        // Check that correct number of arguments are given, validate and save arguments
        if (args.length == 3) {
            // Check that hostname is valid
            if (ScpProtocol.isValidHostname(args[0])) {
                LOCAL_HOSTNAME = args[0];
            } else {
                System.exit(-1);
            }
            // Check that port number is valid
            if (ScpProtocol.isValidPort(args[1])) {
                LOCAL_PORT = Integer.parseInt(args[1]);
            } else {
                System.exit(-1);
                return;
            }
            // Welcome message has no validity constraints
            WELCOME_MSG = args[2];
        } else {
            LOCAL_HOSTNAME = ScpProtocol.default_hostname;
            LOCAL_PORT = ScpProtocol.default_port;
            WELCOME_MSG = ScpProtocol.default_welcome_message;
        }

        // ScpSession Object for this instance
        ScpSession session = new ScpSession();
        session.setLocalHostname(LOCAL_HOSTNAME);
        session.setLocalPort(LOCAL_PORT);

        // Declare program state
        ScpProtocol.state state = ScpProtocol.state.disconnected;

        // Try to open IO resources
        System.out.println("Opening socket server...");
        ServerSocket serverSocket = null;
        System.out.println("Waiting for socket connection...");
        System.out.println();
        Socket socket = null;
        PrintWriter outgoing = null;
        BufferedReader incoming = null;
        Scanner userInput = null;
        try {
            serverSocket = new ServerSocket(LOCAL_PORT);
            socket = serverSocket.accept();
            outgoing = new PrintWriter(socket.getOutputStream(), true);
            incoming = new BufferedReader(new InputStreamReader((socket.getInputStream())));
            userInput = new Scanner(System.in);
        } catch (IOException e) {
            e.printStackTrace();
            state = ScpProtocol.state.exiting;
        }

        // Start program loop
        while (state != ScpProtocol.state.exiting) {
            session.setRemoteHostname(socket.getInetAddress().toString());
            session.setRemotePort(socket.getPort());
            // todo implement this code better without repeated blocks of code
            // todo check all the regexes
            // Start checking for SCP CONNECT message
            if (state == ScpProtocol.state.disconnected) {
                System.out.println("Waiting for CONNECT message");
                // SCP CONNECT validation
                session.setLatest(incoming.readLine());
                if (!session.latest().matches("(SCP CONNECT)")) {
                    System.out.println(ScpProtocol.malformedMessage("SCP Connect", session.latest()));
                    state = ScpProtocol.state.exiting;
                    break;
                }
                // SERVERADDRESS field
                session.setLatest(incoming.readLine());
                if (session.latest().matches("^(SERVERADDRESS )")) {
                    session.setRemoteHostname(session.latest().substring("SERVERADDRESS ".length(), session.latest().length()));
                } else {
                    System.out.println(ScpProtocol.malformedMessage("SERVERADDRESS <hostname>", session.latest()));
                    state = ScpProtocol.state.exiting;
                    break;
                }
                // SERVERPORT field
                session.setLatest(incoming.readLine());
                if (session.latest().matches("^(SERVERPORT )[0-9]+")) {
                    session.setRemotePort(Integer.parseInt(session.latest().substring("SERVERPORT ".length(), session.latest().length())));
                } else {
                    System.out.println(ScpProtocol.malformedMessage("SERVERPORT <serverport>", session.latest()));
                    state = ScpProtocol.state.exiting;
                    break;
                }
                // REQUESTCREATED field
                session.setLatest(incoming.readLine());
                if (session.latest().matches("(REQUESTCREATED )[0-9]+") &&
                        session.latest().substring("REQUESTCREATED ".length(), session.latest().length() - 1).matches("[0-9]*")) {
                    // Check time differential
                    session.setTimeConnectInitiated(Integer.parseInt(session.latest().substring("REQUESTCREATED ".length(), session.latest().length())));
                } else {
                    System.out.println(ScpProtocol.malformedMessage("REQUESTCREATED <timerequestcreated>", session.latest()));
                    state = ScpProtocol.state.exiting;
                    break;
                }
                // USERNAME field
                session.setLatest(incoming.readLine());
                if (session.latest().matches("^(USERNAME ) ")) {
                    session.setClientUsername(session.latest().substring("USERNAME \"".length(), session.latest().length() - 1));
                } else {
                    System.out.println(ScpProtocol.malformedMessage("USERNAME \"<username>\"", session.latest()));
                    state = ScpProtocol.state.exiting;
                    break;
                }
                // SCP END validation
                session.setLatest(incoming.readLine());
                if (!session.latest().matches("(SCP END)")) {
                    System.out.println(ScpProtocol.malformedMessage("SCP END", session.latest()));
                    state = ScpProtocol.state.exiting;
                    break;
                }
                // At this point,
                System.out.println("CONNECT MESSAGE successfully received.");
                state = ScpProtocol.state.connected;
                continue;
            }
            // Send REJECT or send ACCEPT, get ACKNOWLEDGE, send welcome CHAT
            else if (state == ScpProtocol.state.connected) {
                // Check REJECT condition
                long time_differential = System.currentTimeMillis() - session.getTimeConnectInitiated();
                if (time_differential > 5) {
                    // Send REJECT message and return to waiting for connection
                    System.out.println("Time differential too great. Sending REJECT message.");
                    outgoing.print(ScpProtocol.REJECT(time_differential, session.getRemoteHostname()));
                    System.out.println("Sent REJECT message, now waiting for CONNECT");
                    state = ScpProtocol.state.disconnected;
                    continue;
                }

                // Send ACCEPT message
                System.out.println("Sending ACCEPT message.");
                outgoing.println(ScpProtocol.ACCEPT(session.getLocalHost(), session.getRemoteHostname(), session.getRemotePort()));
                System.out.println("Sent ACCEPT message. Waiting for ACKNOWLEDGEMENT message.");

                // Receive ACKNOWLEDGE MESSAGE
                session.setLatest(incoming.readLine());
                if (!session.latest().matches("(SCP ACKNOWLEDGE)")) {
                    System.out.println(ScpProtocol.malformedMessage("SCP ACKNOWLEDGE", session.latest()));
                    state = ScpProtocol.state.exiting;
                    break;
                }
                // Check ACK username
                session.setLatest(incoming.readLine());
                if (session.latest().matches("^(USERNAME )")) {
                    String received_data = session.latest().substring("USERNAME \"".length(), session.latest().length() - 1);
                    if (!received_data.equalsIgnoreCase(session.getLocalHost())) {
                        // Username doesn't match connected username
                        System.out.println("Username is not from current session. Expected: \"" +
                                session.getLocalHost() + "\" but got \"" + received_data + "\"");
                        state = ScpProtocol.state.exiting;
                        break;
                    }
                } else {
                    // Message is malformed
                    System.out.println(ScpProtocol.malformedMessage("USERNAME \"<username>\"", session.latest()));
                    state = ScpProtocol.state.exiting;
                    break;
                }
                // Check ACK server address
                session.setLatest(incoming.readLine());
                if (session.latest().matches("^(SERVERADDRESS )")) {
                    String received_data = session.latest().substring("SERVERADDRESS ".length(), session.latest().length());
                    if (!received_data.equalsIgnoreCase(session.getLocalHost())) {
                        // Server hostname doesn't match connected username
                        System.out.println("Server address specified does not match this server. Expected: " +
                                session.getLocalHost() + " but got " + received_data);
                        state = ScpProtocol.state.exiting;
                        break;
                    }
                } else {
                    // Message is malformed
                    System.out.println(ScpProtocol.malformedMessage("SERVERADDRESS <server hostname>", session.latest()));
                    state = ScpProtocol.state.exiting;
                    break;
                }
                // Check ACK server port
                session.setLatest(incoming.readLine());
                if (session.latest().matches("^(SERVERPORT )[0-9]+")) {
                    int received_data = Integer.parseInt(session.latest().substring("SERVERPORT ".length(), session.latest().length()));
                    if (received_data != session.getLocalPort()) {
                        // Server hostname doesn't match connected username
                        System.out.println("Server port specified does not match this port. Expected: " +
                                session.getLocalPort() + " but got " + received_data);
                        state = ScpProtocol.state.exiting;
                        break;
                    }
                } else {
                    // Message is malformed
                    System.out.println(ScpProtocol.malformedMessage("SERVERPORT <server port>", session.latest()));
                    state = ScpProtocol.state.exiting;
                    break;
                }
                // Check SCP END
                session.setLatest(incoming.readLine());
                if (!session.latest().matches("(SCP END)")) {
                    System.out.println(ScpProtocol.malformedMessage("SCP END", session.latest()));
                    state = ScpProtocol.state.exiting;
                    break;
                }
                System.out.println("Received ACKNOWLEDGE message");

                // Send CHAT welcome message
                System.out.println("Sending CHAT welcome message: \"" + WELCOME_MSG + "\"");
                outgoing.println(ScpProtocol.CHAT(session.getRemoteHostname(), session.getRemotePort(), WELCOME_MSG));
                System.out.println("Sent CHAT welcome message.");

                // Transition to the regular CHAT loop
                state = ScpProtocol.state.chatting;
                continue;
            } else if (state == ScpProtocol.state.chatting) {
                // Check SCP CHAT/DISCONNECT header
                System.out.println("Waiting for incoming message...");
                session.setLatest(incoming.readLine());
                if (session.latest().matches("(SCP CHAT)")) {
                    // Check REMOTEADDRESS matches this address
                    session.setLatest(incoming.readLine());
                    if (session.latest().matches("^(REMOTEADDRESS )")) {
                        String received_data = session.latest().substring("REMOTEADDRESS ".length(), session.latest().length());
                        if (!received_data.equalsIgnoreCase(session.getLocalHost())) {
                            // Server hostname doesn't match connected username
                            System.out.println("Server address specified does not match this server. Expected: " +
                                    session.getLocalHost() + " but got " + received_data);
                            state = ScpProtocol.state.exiting;
                            break;
                        }
                    } else {
                        // Message is malformed
                        System.out.println(ScpProtocol.malformedMessage("REMOTEADDRESS <server hostname>", session.latest()));
                        state = ScpProtocol.state.exiting;
                        break;
                    }
                    // Check REMOTEPORT matches this address
                    session.setLatest(incoming.readLine());
                    if (session.latest().matches("^(REMOTEPORT )[0-9]+")) {
                        int received_data = Integer.parseInt(session.latest().substring("REMOTEPORT ".length(), session.latest().length()));
                        if (received_data != session.getLocalPort()) {
                            // Server hostname doesn't match connected username
                            System.out.println("Server port specified does not match this port. Expected: " +
                                    session.getLocalPort() + " but got " + received_data);
                            state = ScpProtocol.state.exiting;
                            break;
                        }
                    } else {
                        // Message is malformed
                        System.out.println(ScpProtocol.malformedMessage("REMOTEPORT <server port>", session.latest()));
                        state = ScpProtocol.state.exiting;
                        break;
                    }
                    // Check MESSAGECONTENT is valid
                    session.setLatest(incoming.readLine()); // should be MESSAGECONTENT
                    if (!session.latest().matches("(MESSAGECONTENT)")) {
                        System.out.println(ScpProtocol.malformedMessage("MESSAGECONTENT", session.latest()));
                        state = ScpProtocol.state.exiting;
                        break;
                    }
                    // Check for double line break
                    session.setLatest(incoming.readLine()); // should be empty from first \n
                    if (!session.latest().isEmpty()) {
                        System.out.println(ScpProtocol.malformedMessage("<line break>", session.latest()));
                        state = ScpProtocol.state.exiting;
                        break;
                    }
                    session.setLatest(incoming.readLine()); // should be empty from second \n
                    if (!session.latest().isEmpty()) {
                        System.out.println(ScpProtocol.malformedMessage("<line break>", session.latest()));
                        state = ScpProtocol.state.exiting;
                        break;
                    }
                } else if (session.latest().matches("(SCP DISCONNECT)")) {
                    session.setLatest(incoming.readLine());
                    if (!session.latest().matches("(SCP END)")) {
                        // Malformed DISCONNECT
                        System.out.println(ScpProtocol.malformedMessage("SCP END", session.latest()));
                        state = ScpProtocol.state.exiting;
                        break;
                    }
                    System.out.println("Client is disconnecting...");
                    System.out.println("Sending acknowledgement...");
                    outgoing.println(ScpProtocol.ACKNOWLEDGE());
                    System.out.println("Sent acknowledgement of disconnect.");
                    state = ScpProtocol.state.exiting;
                    break;
                } else {
                    //malformed message
                    System.out.println(ScpProtocol.malformedMessage("SCP CHAT or SCP DISCONNECT", session.latest()));
                    state = ScpProtocol.state.exiting;
                    break;
                }

                // Send a CHAT
                System.out.println("Enter a message to send, or DISCONNECT to disconnect: ");
                String outgoing_message = userInput.nextLine();
                if (outgoing_message.equalsIgnoreCase("disconnect")) {
                    // Send DISCONNECT
                    System.out.println("Disconnecting...");
                    outgoing.println(ScpProtocol.DISCONNECT());

                    // Receive ACKNOWLEDGE
                    session.setLatest(incoming.readLine());
                    if (!session.latest().matches("(SCP ACKNOWLEDGE)")) {
                        System.out.println(ScpProtocol.malformedMessage("SCP ACKNOWLEGE", session.latest()));
                        state = ScpProtocol.state.exiting;
                        break;
                    }
                    session.setLatest(incoming.readLine());
                    if (!session.latest().matches("(SCP END)")) {
                        System.out.println(ScpProtocol.malformedMessage("SCP END", session.latest()));
                        state = ScpProtocol.state.exiting;
                        break;
                    }

                    // Exit
                    System.out.println("Disconnect acknowledged. Exiting...");
                    state = ScpProtocol.state.exiting;
                    break;
                } else {
                    System.out.println("Sending that message");
                    outgoing.println(ScpProtocol.CHAT(session.getRemoteHostname(), session.getRemotePort(), outgoing_message));
                    System.out.println("Message sent.");

                    // Continue to next loop of the chat
                    state = ScpProtocol.state.chatting;
                    continue;
                }

            }
        }

        // Close resources
        try {
            if (userInput != null) {
                userInput.close();
            }
            if (incoming != null) {
                incoming.close();
            }
            if (outgoing != null) {
                outgoing.close();
            }
            if (socket != null) {
                socket.close();
            }
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Could not exit without error.");
        }
    }
}