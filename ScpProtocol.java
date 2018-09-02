// Date: 31/08/2018
// This contains methods that are used to format SCP message strings.
public class ScpProtocol {

    // SCP Worker
    public static final String default_hostname = "127.0.0.1";
    // Author: Jordan Maddock
    public static final int default_port = 3400;

    /**
     * Formats a SCP REJECT message
     *
     * @param time_difference unacceptable time difference
     * @param remote_address  address of client being rejected
     * @return formatted string
     */
    public static String REJECT(long time_difference, String remote_address) {
        return ("SCP REJECT\n" +
                "TIMEDIFFERENTIAL " + time_difference + "\n" +
                "REMOTEADDRESS " + remote_address + "\n" +
                "SCP END"
        );
    }

    public static final String default_welcome_message = "Welcome to SCP";

    /**
     * Takes hostname and returns true if it is valid
     * Will print to terminal output if error is found
     *
     * @param hostname a hostname
     * @return boolean true if valid
     */
    public static boolean isValidHostname(String hostname) {
        // Check for illegal spaces in hostname
        if (hostname.contains(" ")) {
            System.out.println("Invalid client hostname. Hostname should not contain spaces. " +
                    "Given argument was:" + hostname);
            return false;
        }
        // Hostname is valid
        return true;
    }

    /**
     * Takes a value for a port number and returns true if it is valid
     * Port numbers should be > 1023 and < 65535, and only contain digits
     * Will print to terminal output if error is found
     *
     * @param portnumber a port number value
     * @return true if value is a valid port number
     */
    public static boolean isValidPort(String portnumber) {
        // Check that port number is integer
        if (!portnumber.matches("[0-9]")) {
            System.out.println("Invalid port number. Expected an integer value. Given argument was: " + portnumber);
            return false;
        }
        // Check that port number is within limits imposed by system
        if (Integer.parseInt(portnumber) < 1023 || Integer.parseInt(portnumber) > 65535) {
            System.out.println("Invalid port number. Port numbers should be between 1023 and 65535. " +
                    "Given port number was " + portnumber);
            return false;
        }
        // Port number is valid
        return true;
    }

    /**
     * Formats a SCP CONNECT message
     *
     * @param server_address  destination hostname
     * @param server_port     destination port
     * @param request_created time request created
     * @param username        client username
     * @return formatted string
     */
    public static String CONNECT(String server_address, int server_port, long request_created, String username) {
        return ("SCP CONNECT\n" +
                "SERVERADDRESS " + server_address + "\n" +
                "SERVERPORT " + server_port + "\n" +
                "REQUESTCREATED " + request_created + "\n" +
                "USERNAME \"" + username + "\"\n" +
                "SCP END"
        );
    }

    // todo comment this method
    public static String malformedMessage(String expectedMessage, String receivedMessage) {
        return "Malformed message received. Expected \"" +
                expectedMessage + "\" but instead got \"" + receivedMessage + "\"";
    }

    /**
     * Formats a SCP ACCEPT message
     *
     * @param username       username of client user
     * @param client_address hostname of client
     * @param client_port    port number used by client
     * @return formatted string
     */
    public static String ACCEPT(String username, String client_address, int client_port) {
        return ("SCP ACCEPT\n" +
                "USERNAME \"" + username + "\"\n" +
                "CLIENTADDRESS " + client_address + "\n" +
                "CLIENTPORT " + client_port + "\n" +
                "SCP END"
        );
    }

    /**
     * Formats a SCP ACKNOWLEDGE message
     *
     * @param username       client username
     * @param server_address server hostname
     * @param server_port    server receiving port
     * @return formatted string
     */
    public static String ACKNOWLEDGE(String username, String server_address, int server_port) {
        return ("SCP ACKNOWLEDGE\n" +
                "USERNAME \"" + username + "\"\n" +
                "SERVERADDRESS " + server_address + "\n" +
                "SERVERPORT " + server_port + "\n" +
                "SCP END"
        );
    }

    /**
     * Formats a short SCP ACKNOWLEDGE message
     *
     * @return formatted string
     */
    public static String ACKNOWLEDGE() {
        return ("SCP ACKNOWLEDGE\n" +
                "SCP END"
        );
    }

    /**
     * Formats a SCP CHAT message
     * The message contents should not contain "SCP END"
     *
     * @param remote_address   destination hostname
     * @param remote_port      destination port
     * @param message_contents contents of chat message
     * @return formatted string
     */
    public static String CHAT(String remote_address, int remote_port, String message_contents) {
        return ("SCP CHAT\n" +
                "REMOTEADDRESS " + remote_address + "\n" +
                "REMOTEPORT " + remote_port + "\n" +
                "MESSAGECONTENT\n" +
                "\n\n" +
                message_contents + "\n" +
                "SCP END"
        );
    }

    /**
     * Formats a SCP DISCONNECT message
     *
     * @return formatted string
     */
    public static String DISCONNECT() {
        return ("SCP DISCONNECT\n" +
                "SCP END"
        );
    }

    /**
     * States that the program can be in during execution
     */
    public enum state {
        disconnected,
        connected,
        chatting,
        exiting
    }
}
