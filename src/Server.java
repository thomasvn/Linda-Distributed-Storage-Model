import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.*;
import java.security.MessageDigest;
import javax.xml.bind.DatatypeConverter;


public class Server implements Runnable {
    // TODO: MAKE GETTERS AND SETTERS
    private static String LOGIN = "tnguyen1";
    private static String HOSTNAME;
    private static String IP_ADDRESS;
    private static int PORT_NUMBER;

    /**
     *
     */
    public Server() {}


    /**
     *
     * @param hostNames
     * @param ipAddresses
     * @param ports
     */
    private static void add(ArrayList<String> hostNames, ArrayList<String> ipAddresses, ArrayList<Integer> ports) {
        try {
            // Create necessary file paths for read/write
            String hostsFilePath = "/tmp/" + LOGIN + "/linda/" + HOSTNAME + "/nets/";
            File dir = new File(hostsFilePath);
            dir.mkdirs();

            // Create files and buffered writers
            hostsFilePath += "hostInfo.txt";
            dir = new File(hostsFilePath);
            Files.deleteIfExists(dir.toPath());
            BufferedWriter bw = new BufferedWriter(new FileWriter(hostsFilePath, true));

            // Append this host's (the master's) configuration information to the file
            bw.write(HOSTNAME + " " + IP_ADDRESS + " " + PORT_NUMBER);
            bw.newLine();
            bw.flush();


            for (int i = 0; i < hostNames.size(); i++) {
                // Connect with host
                Socket s = new Socket();
                s.connect(new InetSocketAddress(ipAddresses.get(i), ports.get(i)));

                // Create a string with host's IP Address and Port Number
                String hostInfo = hostNames.get(i) + " " + ipAddresses.get(i) + " " + ports.get(i);
                System.out.println("added: " + hostInfo);

                // Append this string to a file in /tmp/<login>/linda/<hostName>/nets
                bw.write(hostInfo);
                bw.newLine();
                bw.flush();

                // TODO: /tmp/<login>, /tmp/<login>/linda, /tmp/<login>/linda/<name>/nets --> mode 777
                // TODO: nets and tuples --> mode 666
                s.close();
            }

            bw.close();
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
     *
     * @param rawCommand
     */
    private static void parseUserCommand(String rawCommand) {
        // Remove all spaces
        rawCommand = rawCommand.replace(" ","");

        // Match all regex provided in the pattern. This will group tokens by the "()" delimiters
        Matcher parsedCommand = Pattern.compile("([^()]+)").matcher(rawCommand);

        // Search through the parsed command
        if (parsedCommand.find()) {

            // "add" command was inputted in Linda
            if (parsedCommand.group(0).equalsIgnoreCase("add")) {
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

            // "in" command was inputted in Linda
            else if (parsedCommand.group(0).equalsIgnoreCase("in")) {
                System.out.println(rawCommand.substring(parsedCommand.start(), parsedCommand.end()));
            }

            // "rd" command was inputted in Linda
            else if (parsedCommand.group(0).equalsIgnoreCase("rd")) {
                System.out.println(rawCommand.substring(parsedCommand.start(), parsedCommand.end()));
            }

            // "out" command was inputted in Linda
            else if (parsedCommand.group(0).equalsIgnoreCase("out")) {
                System.out.println(rawCommand.substring(parsedCommand.start(), parsedCommand.end()));
                if (parsedCommand.find()) {
                    getHostID(rawCommand.substring(parsedCommand.start(), parsedCommand.end()));
                }
            }
        }
    }


    /**
     *
     * @param rawTuple
     * @return
     */
    private static int getHostID(String rawTuple) {
        String hashedTuple = hash(rawTuple);
        int hostID = hexToDecimal(hashedTuple);

        // Calculate the number of hosts by reading the number of lines in the file
        int lines = 0;
        try {
            String hostsFilePath = "/tmp/" + LOGIN + "/linda/" + HOSTNAME + "/nets/hostInfo.txt";
            BufferedReader reader = new BufferedReader(new FileReader(hostsFilePath));
            while (reader.readLine() != null) {
                lines++;
            }
            reader.close();
        } catch(java.io.IOException e) {
            System.out.println(e.getStackTrace());
        }

        // Mod the md5 hash with by the number of hosts in the distributed system
        hostID %= lines;

        System.out.println("Host ID: " + hostID);

        return hostID;
    }


    /**
     *
     * @param s
     * @return
     */
    private static int hexToDecimal(String s) {
        String digits = "0123456789ABCDEF";
        s = s.toUpperCase();
        int val = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int d = digits.indexOf(c);
            val = 16 * val + d;
        }
        if (val < 0) val *= -1;
        return val;
    }


    /**
     * This method is implemented assuming we are given a tuple that begins with "(" and ends with ")"
     * @param rawTuple
     * @return
     */
    private static String hash(String rawTuple) {
        String md5Hash = null;

        // Remove extra delimiters in the raw tuple
        rawTuple = rawTuple.replace("(", "");
        rawTuple = rawTuple.replace(")", "");
        rawTuple = rawTuple.replace(",", "");
        rawTuple = rawTuple.replace(" ", "");

        // Hash using the MD5 Sum
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(rawTuple.getBytes());
            md5Hash = new BigInteger(1, md.digest()).toString(16);
            System.out.println("Hash: " + md5Hash);
        } catch(NoSuchAlgorithmException e) {
            System.out.println(e.getStackTrace());
        }

        return md5Hash;
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
        Thread lindaTerminal;

        try {
            HOSTNAME = args[0];
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Please specify the host name when running the executable");
            return;
        }

        try {
            // Create a TCP server socket on a random available port
            while(true) {
                PORT_NUMBER = ThreadLocalRandom.current().nextInt(1024, 65535 + 1);
                try {
                    host = new ServerSocket(PORT_NUMBER);
                    break;
                } catch(IOException e) {
                    continue;
                }
            }

            // Get & display IP of the current machine
            IP_ADDRESS = InetAddress.getLocalHost().getHostAddress();
            System.out.println(IP_ADDRESS + " at port number: " + PORT_NUMBER);

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
