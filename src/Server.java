import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.nio.file.Files;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class Server implements Runnable {
    // TODO: MAKE GETTERS AND SETTERS
    private static String LOGIN = "tnguyen1";
    private static String HOSTNAME;
    private static String IP_ADDRESS;
    private static int PORT_NUMBER;
    private static String listOfHosts = "";

    /**
     *
     */
    public Server() {}


    /**
     * Appends all host names, ip addresses, and ports in the network to the file `hostInfo.txt`
     */
    private static void add()   {
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

            // Parse `listOfHosts` string and add to text file
            String[] hostInfo = listOfHosts.split(",");

            for (int i = 0; i < hostInfo.length; i++) {
                bw.write(hostInfo[i]);
                bw.newLine();
                bw.flush();
            }

            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Makes sure that all hosts in the network have the same `hostInfo.txt` file in the nets directory. This method
     * would only be called by the "master" host
     */
    private static void allHostsAddEachother() {
        String ipAddr;
        int portNum;

        // Split `listOfHosts` by the commas
        String[] hostInfo = listOfHosts.split(",");

        // Send the string `listOfHosts` to all hosts in the network
        for (int i = 0; i < hostInfo.length; i++) {
            // Split `hostInfo[]` by the spaces to Host Names, IP Addresses, Ports
            String[] specificHostInfo = hostInfo[i].split(" ");
            ipAddr = specificHostInfo[1];
            portNum = Integer.parseInt(specificHostInfo[2]);

            try {
                Socket s = new Socket();
                s.connect(new InetSocketAddress(ipAddr, portNum));

                // Sending the string `listOfHosts` to specific host
                OutputStream os = s.getOutputStream();
                os.write(listOfHosts.getBytes());
                os.close();

                s.close();
            } catch(IOException e) {
                System.out.println(e.getStackTrace());
            }
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
    private static void parseLindaCommand(String rawCommand) {
        // Remove all spaces
        rawCommand = rawCommand.replace(" ","");

        // Match all regex provided in the pattern. This will group tokens by the "()" delimiters
        Matcher parsedCommand = Pattern.compile("([^()]+)").matcher(rawCommand);

        // Search through the parsed command
        if (parsedCommand.find()) {

            // "add" command was inputted in Linda
            if (parsedCommand.group(0).equalsIgnoreCase("add")) {
                // Add this host to a String managed by this Server Instance
                listOfHosts += (HOSTNAME + " " + IP_ADDRESS + " " + PORT_NUMBER + ",");

                // Place all host information in ArrayLists and add these hosts
                while (parsedCommand.find()) {
                    // Split into array by using commas as delimiters
                    String[] tokenizedCommand = rawCommand.substring(parsedCommand.start(), parsedCommand.end()).split(",");

                    // Add this host to a String managed by this Server Instance
                    listOfHosts += (tokenizedCommand[0] + " " + tokenizedCommand[1] + " " + tokenizedCommand[2] + ",");
                }
                add();
                allHostsAddEachother();
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
        // TODO: Calculate the number of lines by splitting `listOfHosts` by the comma operator
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
                parseLindaCommand(commandLine);
            }
        }
    }


    /**
     *
     * @param args
     */
    public static void main(String [] args) {
        ServerSocket serverSocket;
        Socket socket;
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
                    serverSocket = new ServerSocket(PORT_NUMBER);
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
                socket = serverSocket.accept();
                System.out.println("\nA Connection has been established!");

                // Read the input stream of messages from other hosts
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String output = br.readLine();

                // Add the list of hosts
                listOfHosts = output;
                add();
                socket.close();
            }

            // Close the sockets?
            // TODO: CHeck for when input stream is null. Once it is, then we close the socket
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // TODO: /tmp/<login>, /tmp/<login>/linda, /tmp/<login>/linda/<name>/nets --> mode 777
    // TODO: nets and tuples --> mode 666
}
