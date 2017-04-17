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
    private static String LOGIN = "tnguyen1";
    private static String HOSTNAME;
    private static String IP_ADDRESS;
    private static int PORT_NUMBER;
    private static String listOfHosts = "";


/************************************************ Linda Commands ******************************************************/
    /**
     * Appends all host names, ip addresses, and ports in the network to the file `hostInfo.txt`
     */
    private void add() {
        try {
            // Create necessary file paths for the host's tuple space
            String tupleSpaceFilePath = "/tmp/" + LOGIN + "/linda/" + HOSTNAME + "/tuples/";
            File dir = new File(tupleSpaceFilePath);
            dir.mkdirs();

            // Create necessary file paths for maintaining host information
            String hostsFilePath = "/tmp/" + LOGIN + "/linda/" + HOSTNAME + "/nets/";
            dir = new File(hostsFilePath);
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
     *
     * @param hashedTuple
     */
    private void in(String hashedTuple) {

    }


    /**
     *
     * @param rawTuple
     */
    private void rd(String rawTuple) {
        String hostInfo = "";

        try {
            String hostsFilePath = "/tmp/" + LOGIN + "/linda/" + HOSTNAME + "/nets/hostInfo.txt";
            BufferedReader reader = new BufferedReader(new FileReader(hostsFilePath));

            // Broadcast new tuple to all hosts in the network
            while ((hostInfo = reader.readLine()) != null) {
                // Parse information about the host
                String[] specificHostInfo = hostInfo.split(" ");
                String ipAddr = specificHostInfo[1];
                int portNum = Integer.parseInt(specificHostInfo[2]);

                // Create a socket connection to the correct host
                Socket s = new Socket();
                s.connect(new InetSocketAddress(ipAddr, portNum));

                // Send a message in the datastream to write to the TupleSpace
                OutputStream os = s.getOutputStream();
                String outputMessage = "rd~" + rawTuple;
                System.out.println("Output Message (Host " + getHostID(rawTuple) + "): " + outputMessage);
                os.write(outputMessage.getBytes());
                os.close();

                s.close();
            }

            // TODO: BLOCK UNTIL IT RECEIVES AN ACK FOR THIS SPECIFIC TUPLE

            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     *
     * @param rawTuple
     */
    private void out(String rawTuple) {
        String hostInfo = null;

        // Instantiate the tuple object
        // TODO: Create a method to check if it is a valid tuple?
        Tuple tuple = new Tuple(rawTuple);

        try {
            // Retrieve the information of the correct host
            String hostsFilePath = "/tmp/" + LOGIN + "/linda/" + HOSTNAME + "/nets/hostInfo.txt";
            BufferedReader reader = new BufferedReader(new FileReader(hostsFilePath));

            // Goes to the line in the text file of the correct host
            int hostID = getHostID(rawTuple);
            for (int i = 0; i < hostID; i++) {
                reader.readLine();
            }
            hostInfo = reader.readLine();

            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Parse information about the host
        String[] specificHostInfo = hostInfo.split(" ");
        String ipAddr = specificHostInfo[1];
        int portNum = Integer.parseInt(specificHostInfo[2]);

        try {
            // Create a socket connection to the correct host
            Socket s = new Socket();
            s.connect(new InetSocketAddress(ipAddr, portNum));

            // Send a message in the datastream to write to the TupleSpace
            OutputStream os = s.getOutputStream();
            String outputMessage = "out~" + rawTuple;
            System.out.println("Output Message (Host " + getHostID(rawTuple) + "): " + outputMessage);
            os.write(outputMessage.getBytes());
            os.close();

            s.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }



/************************************************** Parsing Input *****************************************************/
    /**
     *
     * @param command
     */
    private void parseLindaCommand(String command) {
        // TODO: Watch out for removing spaces in strings
        // Remove all spaces
        command = command.replace(" ","");

        // Match all regex provided in the pattern. This will group tokens by the "()" delimiters
        Matcher parsedCommand = Pattern.compile("([^()]+)").matcher(command);

        // Search through the parsed command
        if (parsedCommand.find()) {

            // "add" command was inputted in Linda
            if (parsedCommand.group(0).equalsIgnoreCase("add")) {
                // Place all host information in ArrayLists and add these hosts
                while (parsedCommand.find()) {
                    // Split into array by using commas as delimiters
                    String[] tokenizedCommand = command.substring(parsedCommand.start(), parsedCommand.end()).split(",");

                    // Add this host to a String managed by this Server Instance
                    listOfHosts += (tokenizedCommand[0] + " " + tokenizedCommand[1] + " " + tokenizedCommand[2] + ",");
                }
                add();
                allHostsAddEachOther();
            }

            // "out" command was inputted in Linda
            else if (parsedCommand.group(0).equalsIgnoreCase("out")) {
                if (parsedCommand.find()) {
                    out(command.substring(parsedCommand.start(), parsedCommand.end()));
                }
            }

            // "in" command was inputted in Linda
            else if (parsedCommand.group(0).equalsIgnoreCase("in")) {
                System.out.println(command.substring(parsedCommand.start(), parsedCommand.end()));
            }

            // "rd" command was inputted in Linda
            else if (parsedCommand.group(0).equalsIgnoreCase("rd")) {
                if (parsedCommand.find()) {
                    rd(command.substring(parsedCommand.start(), parsedCommand.end()));
                }
            }

        }
    }


    /**
     *
     * @param command
     */
    private void parseDataStreamCommand(String command) {
        // Parse the DataStream Command
        String[] parsedCommand = command.split("~");

        if (parsedCommand[0].equals("add")) {
            listOfHosts = parsedCommand[1];
            add();
            System.out.print("A Connection has been established!");
        } else if (parsedCommand[0].equals("out")) {
            try {
                // Open a writer to the tuples file on this host
                String tupleSpaceFilePath = "/tmp/" + LOGIN + "/linda/" + HOSTNAME + "/tuples/tuples.txt";
                BufferedWriter bw = new BufferedWriter(new FileWriter(tupleSpaceFilePath, true));

                // Write the tuple into the host's file
                bw.write(parsedCommand[1]);
                bw.newLine();
                bw.flush();
                bw.close();

                System.out.print("Tuple has been added to this host: (" + parsedCommand[1] + ")");
            } catch(IOException e) {
                e.printStackTrace();
            }
        } else if (parsedCommand[0].equals("rd")) {
            System.out.println("RD this tuple: (" + parsedCommand[1] + ")");
            // Need to send an ACK back if found in the Tuple Space
        }
    }



/******************************************* Miscellaneous Helper Methods *********************************************/
    /**
     * Makes sure that all hosts in the network have the same `hostInfo.txt` file in the nets directory. This method
     * would only be called by the "master" host
     */
    private void allHostsAddEachOther() {
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
                String outputMessage = "add~" + listOfHosts;
                os.write(outputMessage.getBytes());
                os.close();

                s.close();
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     *
     * @param rawTuple
     * @return
     */
    private int getHostID(String rawTuple) {
        String hashedTuple = hash(rawTuple);
        int hostID = hexToDecimal(hashedTuple);

        // Calculate the number of hosts currently accounted for
        String[] hostInfo = listOfHosts.split(",");
        int lines = hostInfo.length;

        // Mod the md5 hash with by the number of hosts in the distributed system
        hostID %= lines;

        return hostID;
    }


    /**
     *
     * @param s
     * @return
     */
    private int hexToDecimal(String s) {
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
    private String hash(String rawTuple) {
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
        } catch(NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return md5Hash;
    }



/**************************************** Methods for Linda Thread & Socket Thread ************************************/
    /**
     * This method is messaged when creating a new thread for our Linda terminal.
     * This method will be handling all Linda Terminal commands `add()` `out()` `in()` `rd()`
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
     * This method handles input from other servers.
     * @param args
     */
    public void listener(String[] args) {
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

            // Remove old tuple space if it exists
            String tupleSpaceFilePath = "/tmp/" + LOGIN + "/linda/" + HOSTNAME + "/tuples/tuples.txt";
            File dir = new File(tupleSpaceFilePath);
            Files.deleteIfExists(dir.toPath());

            // Add this host to the list of hosts in our network
            listOfHosts += (HOSTNAME + " " + IP_ADDRESS + " " + PORT_NUMBER + ",");
            add();

            // Create a new thread to accept Linda Terminal Commands
            lindaTerminal = new Thread(new Server());
            lindaTerminal.start();

            // Listen for new socket connections to from hosts that request it
            while (true) {
                socket = serverSocket.accept();

                // Read the input stream of messages from other hosts
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String dataStreamCommand = br.readLine();
                parseDataStreamCommand(dataStreamCommand);

                socket.close();
            }

            // Close the sockets?
            // TODO: CHeck for when input stream is null. Once it is, then we close the socket
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        Server host = new Server();
        host.listener(args);
    }
}
