import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.*;


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
    private static void add(ArrayList<String> hostNames, ArrayList<String> ipAddresses, ArrayList<Integer> ports) {
        try {
            for (int i = 0; i < hostNames.size(); i++) {
                // Connect with host
                Socket s = new Socket();
                s.connect(new InetSocketAddress(ipAddresses.get(i), ports.get(i)));
                System.out.println(hostNames.get(i) + " on " + ipAddresses.get(i) + " at " + ports.get(i));

                // Create a string with host's IP Address and Port Number

                // Append this string to a file in /tmp/<login>/linda/<hostName>/nets


                // TODO: /tmp/<login>, /tmp/<login>/linda, /tmp/<login>/linda/<name>/nets --> mode 777
                // TODO: nets and tuples --> mode 666
                s.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        // Remove all spaces
        rawCommand = rawCommand.replace(" ","");

        // Match all regex provided in the pattern. This should return command without "()" delimiters
        Matcher parsedCommand = Pattern.compile("([^()]+)").matcher(rawCommand);

        // Search through the parsed command
        if (parsedCommand.find()) {

            // Run this code block if it identifies the "add" command
            if (parsedCommand.group(0).equalsIgnoreCase("add")) {
                System.out.println(rawCommand.substring(parsedCommand.start(), parsedCommand.end()));

                ArrayList<String> hostNames = new ArrayList<String>();
                ArrayList<String> ipAddresses = new ArrayList<String>();
                ArrayList<Integer> ports = new ArrayList<Integer>();

                // Place all host information in ArrayLists and add these hosts
                while (parsedCommand.find()) {
                    // Split into array by using commas as delimiters
                    String[] tokenizedCommand = rawCommand.substring(parsedCommand.start(), parsedCommand.end()).split(",");

                    hostNames.add(tokenizedCommand[0]);
                    ipAddresses.add(tokenizedCommand[1]);
                    ports.add(Integer.parseInt(tokenizedCommand[2]));
                }
                add(hostNames, ipAddresses, ports);
            }

            // Run this code block if it identifies the "in" command
            else if (parsedCommand.group(0).equalsIgnoreCase("in")) {
                System.out.println(rawCommand.substring(parsedCommand.start(), parsedCommand.end()));
            }

            // Run this code block if it identifies the "rd" command
            else if (parsedCommand.group(0).equalsIgnoreCase("rd")) {
                System.out.println(rawCommand.substring(parsedCommand.start(), parsedCommand.end()));
            }

            // Run this code block if it identifies the "out" command
            else if (parsedCommand.group(0).equalsIgnoreCase("out")) {
                System.out.println(rawCommand.substring(parsedCommand.start(), parsedCommand.end()));
            }
        }
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
            // TODO: CHeck for when input stream is null. Once it is, then we close the socket
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
