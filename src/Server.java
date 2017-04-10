import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

public class Server implements Runnable {
    /**
     *
     */
    public Server() {}


    /**
     *
     * @param ipAddr
     * @param port
     */
    private void add(String ipAddr, int port) {
        try {
            // Connect with host
            Socket s = new Socket();
            s.connect(new InetSocketAddress(ipAddr, port));

            System.out.println("Connection Successful");

            s.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // include the host's IP address and Port number in /tmp/<login>/linda/<hostName>/nets

        // Traverse through the nets file and make a client request to all other existing hosts to also add the new host
    }


    /**
     *
     * @param hashedTuple
     */
    private static void in(String hashedTuple) {}


    /**
     *
     * @param hashedTuple
     */
    private static void rd(String hashedTuple) {}


    /**
     *
     * @param hashedTuple
     */
    private static void out(String hashedTuple) {}


    /**
     * This method is implemented assuming we are given a tuple that begins with "(" and ends with ")"
     */
    private static String hash(String rawTuple) {
        // TODO how to add quotation marks in quotation marks in the passed rawTuple
        rawTuple = rawTuple.replace("(", "");
        rawTuple = rawTuple.replace(")", "");
        rawTuple = rawTuple.replace(",", "");
        rawTuple = rawTuple.replace(" ", "");

        System.out.println(rawTuple);
        return rawTuple;
    }


    /**
     *
     * @param rawCommand
     */
    private static void parseUserCommand(String rawCommand) {
        // Call add(), in(), rd(), our out() with their respective arguments
        System.out.println(rawCommand);
    }


    /**
     * This method is messaged when creating a new thread for our Linda terminal.
     */
    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        String commandLine;

        // Listening for user commands
        while (true) {
            System.out.print("linda> ");
            commandLine = scanner.nextLine();

            if (commandLine.equals("")) {
                continue;
            } else if (commandLine.equals("exit")) {
                break;
            } else {
                parseUserCommand(commandLine);
            }
        }
    }


    /**
     *
     * @param args
     */
    public static void main(String [] args) {
        ServerSocket host;
        Socket clientSocket;
        DataInputStream input;
        DataOutputStream output;
        InetAddress ipAddr;
        int randPort;
        Thread lindaTerminal;

//        hash("(abc, 3)");

        try {
            // Create a TCP server socket on a random available port
            while(true) {
                randPort = ThreadLocalRandom.current().nextInt(1024, 65535 + 1);
                try {
                    host = new ServerSocket(randPort);
                    break;
                } catch(IOException e) {
                    continue;
                }
            }

            // TODO Establish a port without having to instantiate the ServerSocket
            // Get & display IP of the current machine
            ipAddr = InetAddress.getLocalHost();
            System.out.println(ipAddr.getHostAddress() + " at port number: " + randPort);

            // Create a new thread to accept Linda Terminal Commands
            lindaTerminal = new Thread(new Server());
            lindaTerminal.start();

            // Listen for new socket connections to from hosts that request it
            while (true) {
                clientSocket = host.accept();
                input = new DataInputStream(clientSocket.getInputStream());
                System.out.println("\nA Connection has been established!");
            }

            // Close the sockets?
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
