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
        String HOST_NAME, WELCOME_MSG;
        int PORT_NUMBER;

        // Check that correct number of arguments are given and import arguments
        if (args.length == 3) {
            // Check validity of the hostname and store if valid
            if (args[0].contains(" ")) {
                System.out.println("Invalid client hostname. Given argument was:" + args[0]);
                return;
            } else {
                HOST_NAME = args[0]; // Hostname of client
            }
            // Check for validity of port number and store if valid
            if (!args[1].matches("[0-9]+")) {
                System.out.println("Invalid port number. Expected an integer value. Given argument was: " + args[1]);
                return;
            } else {
                PORT_NUMBER = Integer.parseInt(args[1]); // Port for sending to client
                if (PORT_NUMBER < 1023 || PORT_NUMBER > 65535) {
                    System.out.println("Invalid port number. Port numbers should be between 1023 and 65535. " +
                            "Given port number was " + args[1]);
                    return;
                }
            }
            // Welcome message has no validity constraints
            WELCOME_MSG = args[2]; // Welcome message to send to client
        } else {
            HOST_NAME = "127.0.0.1";
            PORT_NUMBER = 3400;
            WELCOME_MSG = "Welcome to SCP";
        }

        try (ServerSocket serverSocket = new ServerSocket(PORT_NUMBER);
             Socket clientSocket = serverSocket.accept();
             PrintWriter out = new PrintWriter(
                     clientSocket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader((clientSocket.getInputStream())))
        ) {

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