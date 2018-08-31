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

        try (ServerSocket serverSocket = new ServerSocket(LOCAL_PORT);
             Socket clientSocket = serverSocket.accept();
             PrintWriter out = new PrintWriter(
                     clientSocket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader((clientSocket.getInputStream())))
        ) {
            // Start checking for SCP CONNECT message
            String latest_message = in.readLine();
            if (!latest_message.matches("^(SCP CONNECT) $(SCP END)")) {
                System.out.println("Malformed message received. " +
                        "Expected SPC CONNECT, but instead got " + latest_message);
            }


        }

        // create the socket server

        // wait for connection request

        // when receiving connection request, check for time difference
        // if time diff is greater than five seconds, send a rejection
        // if the time difference is acceptable, send an acceptance message

        // wait for acknowledgement
        // send the welcome message

        // enter receive and then chat or disconnect cycle

    }
}