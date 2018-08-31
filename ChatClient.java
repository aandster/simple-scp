// ChatClient
// Author: Jordan Maddock
// Date: 31/08/2018
// This is the client for the simple-scp project.

import java.io.IOException;

public class ChatClient {

    public static void main(String[] args) throws IOException {
        String HOSTNAME;
        int PORT_NUMBER;

        // Check that correct number of arguments are given, validate and save arguments
        if (args.length == 2) {
            // Check that hostname is valid
            if (ScpProtocol.isValidHostname(args[0])) {
                HOSTNAME = args[0];
            } else {
                return;
            }
            // Check that port number is valid
            if (ScpProtocol.isValidPort(args[1])) {
                PORT_NUMBER = Integer.parseInt(args[1]);
            } else {
                return;
            }
        } else {
            HOSTNAME = ScpProtocol.default_hostname;
            PORT_NUMBER = ScpProtocol.default_port;
        }

        // create the socket

        // send connection request to server

        // if rejection message is received, exit the program

        // if acceptance message is received, send an acknowledgement

        // enter receive and then chat or disconnect cycle

        // wait for chat to be received
    }

}