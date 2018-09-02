// ChatClient
// Author: Jordan Maddock
// Date: 31/08/2018
// This is the client for the simple-scp project.

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {

    public static void main(String[] args) throws IOException {
        String LOCAL_HOSTNAME, REMOTE_HOSTNAME = null;
        int LOCAL_PORT, REMOTE_PORT = 0;

        // Check that correct number of arguments are given, validate and save arguments
        if (args.length == 2) {
            // Check that hostname is valid
            if (ScpProtocol.isValidHostname(args[0])) {
                REMOTE_HOSTNAME = args[0];
            } else {
                System.exit(-1);
            }
            // Check that port number is valid
            if (ScpProtocol.isValidPort(args[1])) {
                REMOTE_PORT = Integer.parseInt(args[1]);
            } else {
                System.exit(-1);
            }
        } else {
            REMOTE_HOSTNAME = ScpProtocol.default_hostname;
            REMOTE_PORT = ScpProtocol.default_port;
        }

        // ScpSession Object for this instance
        ScpSession session = new ScpSession();
        session.setLocalHostname(REMOTE_HOSTNAME);
        session.setLocalPort(REMOTE_PORT);

        // Initial program state
        ScpProtocol.state state = ScpProtocol.state.disconnected;

        // Try to open remote resources
        System.out.println("Opening socket...");
        Socket socket = null;
        PrintWriter outgoing = null;
        BufferedReader incoming = null;
        Scanner userInput = null;
        try {
            socket = new Socket(REMOTE_HOSTNAME, REMOTE_PORT);
            outgoing = new PrintWriter(socket.getOutputStream(), true);
            incoming = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            userInput = new Scanner(System.in);
        } catch (IOException e) {
            e.printStackTrace();
            state = ScpProtocol.state.exiting;
        }

        while (state != ScpProtocol.state.exiting) {
            session.setRemoteHostname(socket.getInetAddress().toString());
            session.setLocalPort(socket.getPort());

            // Start checking for SCP CONNECT message
            if (state == ScpProtocol.state.disconnected) {
                // Get username from user
                System.out.println("Input your username: ");
                session.setClientUsername(userInput.nextLine());
                System.out.println("Username saved.");

                // Send CONNECT
                System.out.println("Sending SCP CONNECT");
                outgoing.println(ScpProtocol.CONNECT(session.getRemoteHostname(), session.getRemotePort(), System.currentTimeMillis(), session.getClientUsername()));
                System.out.println("Sent CONNECT message.");

                // Receive REJECT or CHAT
                System.out.println("Waiting for REJECT or ACKNOWLEDGE message from server...");
                session.setLatest(incoming.readLine());
                if (session.latest().matches("SCP REJECT")) {
                    System.out.println("Connection was rejected because time differential was too large. Exiting...");
                    state = ScpProtocol.state.exiting;
                    break;
                } else if (session.latest().matches("(SCP ACKNOWLEDGE)")) {
                    // receive ACCEPT
                    // validate ACCEPT USERNAME
                    session.setLatest(incoming.readLine());
                    if (session.latest().matches("^(USERNAME )")) {
                        String received_data = session.latest().substring("USERNAME \"".length(), session.latest().length() - 1);
                        if (!session.getClientUsername().equals(received_data)) {
                            // Usernames don't match
                            System.out.println("Client username does not match addressed username. Expected \""
                                    + session.getClientUsername() + "\" but got \"" + received_data + "\"");
                            state = ScpProtocol.state.exiting;
                            break;
                        }
                    } else {
                        System.out.println(ScpProtocol.malformedMessage("USERNAME <username>", session.latest()));
                        state = ScpProtocol.state.exiting;
                        break;
                    }
                    // validate ACCEPT CLIENTADDRESS
                    session.setLatest(incoming.readLine());
                    if (session.latest().matches("^(CLIENTADDRESS )")) {
                        String received_data = session.latest().substring("CLIENTADDRESS ".length(), session.latest().length());
                        if (!session.getLocalHost().equals(received_data)) {
                            // Addresses don't match
                            System.out.println("Client address does not match this hostname. Expected "
                                    + session.getLocalHost() + " but got " + received_data);
                            state = ScpProtocol.state.exiting;
                            break;
                        }
                    } else {
                        System.out.println(ScpProtocol.malformedMessage("CLIENTADDRESS <client hostname>", session.latest()));
                        state = ScpProtocol.state.exiting;
                        break;
                    }
                    // Validate ACCEPT CLIENTPORT
                    session.setLatest(incoming.readLine());
                    if (session.latest().matches("^(CLIENTPORT )[0-9]+")) {
                        int received_data = Integer.parseInt(session.latest().substring("CLIENTADDRESS ".length(), session.latest().length()));
                        if (session.getLocalPort() != received_data) {
                            // Ports don't match
                            System.out.println("Client port does not match this port. Expected "
                                    + session.getLocalPort() + " but got " + received_data);
                            state = ScpProtocol.state.exiting;
                            break;
                        }
                    } else {
                        System.out.println(ScpProtocol.malformedMessage("CLIENTPORT <client port>", session.latest()));
                        state = ScpProtocol.state.exiting;
                        break;
                    }
                    // Validate ACCEPT SCP END
                    session.setLatest(incoming.readLine());
                    if (!session.latest().matches("(SCP END)")) {
                        System.out.println(ScpProtocol.malformedMessage("SCP END", session.latest()));
                        state = ScpProtocol.state.exiting;
                        break;
                    }
                    System.out.println("Received ACCEPT message. Sending ACKNOWLEDGE message...");

                    // Send ACKNOWLEDGE
                    outgoing.println(ScpProtocol.ACKNOWLEDGE(session.getClientUsername(), session.getRemoteHostname(), session.getRemotePort()));
                    System.out.println("Sent ACKNOWLEDGE message");

                    // Go to CHAT loop
                    state = ScpProtocol.state.chatting;
                    continue;
                } else {
                    System.out.println(ScpProtocol.malformedMessage("SCP REJECT or SCP CHAT", session.latest()));
                    state = ScpProtocol.state.exiting;
                    break;
                }
            }
            // Chatting loop
            if (state == ScpProtocol.state.chatting) {
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
                            System.out.println("Client address specified does not match this hostname. Expected: " +
                                    session.getLocalHost() + " but got " + received_data);
                            state = ScpProtocol.state.exiting;
                            break;
                        }
                    } else {
                        // Message is malformed
                        System.out.println(ScpProtocol.malformedMessage("REMOTEADDRESS <client hostname>", session.latest()));
                        state = ScpProtocol.state.exiting;
                        break;
                    }
                    // Check REMOTEPORT matches this address
                    session.setLatest(incoming.readLine());
                    if (session.latest().matches("^(REMOTEPORT )[0-9]+")) {
                        int received_data = Integer.parseInt(session.latest().substring("REMOTEPORT ".length(), session.latest().length()));
                        if (received_data != session.getLocalPort()) {
                            // Server hostname doesn't match connected username
                            System.out.println("Client port specified does not match this port. Expected: " +
                                    session.getLocalPort() + " but got " + received_data);
                            state = ScpProtocol.state.exiting;
                            break;
                        }
                    } else {
                        // Message is malformed
                        System.out.println(ScpProtocol.malformedMessage("REMOTEPORT <client port>", session.latest()));
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
                    System.out.println("Server is disconnecting...");
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
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Could not exit without error.");
        }
    }

}