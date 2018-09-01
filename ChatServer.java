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

        // Initiate communications
        /*
         * todo implement the functionality to allow a 'waiting for connection message' to be displayed.
         * - use just a try-catch to open the resources after declaring outside of it
         * - after the main program loop close all the resources that have been opened
         */
        try (ServerSocket serverSocket = new ServerSocket(LOCAL_PORT);
             Socket clientSocket = serverSocket.accept();
             PrintWriter out = new PrintWriter(
                     clientSocket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader((clientSocket.getInputStream())))) {
            ScpProtocol.state state = ScpProtocol.state.disconnected;
            do {
                // todo implement this code better without repeated blocks of code
                // todo check all the regexes
                // Start checking for SCP CONNECT message
                if (state == ScpProtocol.state.disconnected) {
                    session.setLatest(in.readLine());
                    if (!session.latest().matches("(SCP CONNECT)")) {
                        System.out.println(ScpProtocol.malformedMessage("SCP Connect", session.latest()));
                        state = ScpProtocol.state.exiting;
                        break;
                    }
                    // SERVERADDRESS field
                    session.setLatest(in.readLine());
                    if (session.latest().matches("^(SERVERADDRESS )")) {
                        session.setRemoteHostname(session.latest().substring("SERVERADDRESS ".length(), session.latest().length()));
                    } else {
                        System.out.println(ScpProtocol.malformedMessage("SERVERADDRESS <hostname>", session.latest()));
                        state = ScpProtocol.state.exiting;
                        break;
                    }
                    // SERVERPORT field
                    session.setLatest(in.readLine());
                    if (session.latest().matches("^(SERVERPORT )[0-9]+")) {
                        session.setRemotePort(Integer.parseInt(session.latest().substring("SERVERPORT ".length(), session.latest().length())));
                    } else {
                        System.out.println(ScpProtocol.malformedMessage("SERVERPORT <serverport>", session.latest()));
                        state = ScpProtocol.state.exiting;
                        break;
                    }
                    // REQUESTCREATED field
                    session.setLatest(in.readLine());
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
                    session.setLatest(in.readLine());
                    if (session.latest().matches("^(USERNAME \") $(\")")) {
                        session.setUsername(session.latest().substring("USERNAME \"".length(), session.latest().length() - 1));
                    } else {
                        System.out.println(ScpProtocol.malformedMessage("USERNAME \"<username>\"", session.latest()));
                        state = ScpProtocol.state.exiting;
                        break;
                    }
                    // At this point,
                    System.out.println("CONNECT MESSAGE successfully received.");
                    state = ScpProtocol.state.connected;
                    continue;
                }
                if (state == ScpProtocol.state.connected) {
                    /*
                    todo do the reject or accept/acknowledge/chat alt.
                    next state will be chatting or disconnected

                    ALT:
                    - reject: state = disconnected, continue;
                    - send accept, receive and validate acknowledgement, send welcome message, transition to chatting

                    update state;
                    continue;
                    */
                }
                if (state == ScpProtocol.state.chatting) {
                    /*
                    todo do the main chatting part to loop for
                    start out waiting for a chat to be sent

                    RECEIVING CHAT
                    - make sure to include 'waiting for message' code
                    - first off check for header validity and whether CHAT or DISCONNECT
                        - if DISCONNECT, validate, send acknowledgement, and exit.
                        - if CHAT, validate, display message in console and continue to receiving part

                    SENDING CHAT
                    - make sure to include functionality that confirms a message has been sent
                    - first off, get the user input from the terminal and check if it's a message or disconnection
                        - if DISCONNECT, send disconnect, wait for and validate acknowledgement, and return to disconnected
                        - if CHAT, send it off and return to the start of the chating loop

                    -> state changes from here will either be to chatting, disconnected or exiting/break
                    -> this could be split into the receiving and sending parts
                    -> this could be split further to accommodate to the disconnect/acknowledge/chat to reduce indent a bit
                     */
                }
            } while (state != ScpProtocol.state.exiting);
        }
    }
}