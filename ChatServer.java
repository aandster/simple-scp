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
        String LOCAL_HOSTNAME, REMOTE_HOSTNAME, WELCOME_MSG;
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
        try (ServerSocket serverSocket = new ServerSocket(LOCAL_PORT);
             Socket clientSocket = serverSocket.accept();
             PrintWriter out = new PrintWriter(
                     clientSocket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader((clientSocket.getInputStream())))
        ) {
            // todo implement this code better without repeated blocks of code
            // todo check all the regexes
            // Start checking for SCP CONNECT message
            session.setLatest(in.readLine());
            if (!session.latest().matches("(SCP CONNECT)")) {
                System.out.println(ScpProtocol.malformedMessage("SCP Connect", session.latest()));
                System.exit(-1);
            }
            // SERVERADDRESS field
            session.setLatest(in.readLine());
            if (session.latest().matches("^(SERVERADDRESS )")) {
                session.setRemoteHostname(session.latest().substring("SERVERADDRESS ".length(), session.latest().length()));
            } else {
                System.out.println(ScpProtocol.malformedMessage("SERVERADDRESS <hostname>", session.latest()));
                System.exit(-1);
            }
            // SERVERPORT field
            session.setLatest(in.readLine());
            if (session.latest().matches("^(SERVERPORT )[0-9]+")) {
                session.setRemotePort(Integer.parseInt(session.latest().substring("SERVERPORT ".length(), session.latest().length())));
            } else {
                System.out.println(ScpProtocol.malformedMessage("SERVERPORT <serverport>", session.latest()));
                System.exit(-1);
            }
            // REQUESTCREATED field
            session.setLatest(in.readLine());
            if (session.latest().matches("(REQUESTCREATED )[0-9]+") &&
                    session.latest().substring("REQUESTCREATED ".length(), session.latest().length() - 1).matches("[0-9]*")) {
                // Check time differential
                if (System.currentTimeMillis() - Integer.parseInt(session.latest().substring("REQUESTCREATED ".length(), session.latest().length() - 1)) <= 5) {
                    session.setTimeConnectInitiated(Integer.parseInt(session.latest().substring("REQUESTCREATED ".length(), session.latest().length())));
                } else {
                    System.out.println("CONNECT message has expired.");
                    System.exit(-1);
                }
            } else {
                System.out.println(ScpProtocol.malformedMessage("REQUESTCREATED <timerequestcreated>", session.latest()));
                System.exit(-1);
            }
            // USERNAME field
            session.setLatest(in.readLine());
            if (session.latest().matches("^(USERNAME \") $(\")")) {
                session.setUsername(session.latest().substring("USERNAME \"".length(), session.latest().length() - 1));
            } else {
                System.out.println(ScpProtocol.malformedMessage("USERNAME \"<username>\"", session.latest()));
                System.exit(-1);
            }
            System.out.println("CONNECT MESSAGE successfully received.");
        }

        // when receiving connection request, check for time difference
        // if time diff is greater than five seconds, send a rejection
        // if the time difference is acceptable, send an acceptance message

        // wait for acknowledgement
        // send the welcome message

        // enter receive and then chat or disconnect cycle

    }
}