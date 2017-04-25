import java.io.*;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.net.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class P2 implements Runnable {
    private static String LOGIN = "tnguyen1";
    private static String HOSTNAME;
    private static String IP_ADDRESS;
    private static int PORT_NUMBER;
    private static String listOfHosts = "";
    private static boolean blocked = false;
    private static String tupleThatIsBlocking;
    private static ArrayList<String> requestedTuples = new ArrayList<>(); // TODO: Implement this
    private static LookupTable lookupTable;


/************************************************ Linda Commands ******************************************************/
    /**
     * Appends all host names, ip addresses, and ports in the network to the file `hostInfo.txt`.
     *
     * Updates our lookup table and tells all other nodes to update their lookup tables as well
     *
     * NOTE: the node that is being added will only join the network if a node that is currently in the network is
     * adding it in
     */
    private void add() {
        try {
            // Create necessary file paths for maintaining host information
            String hostsFilePath = "/tmp/" + LOGIN + "/linda/" + HOSTNAME + "/nets/";
            File dir = new File(hostsFilePath);
            dir.mkdirs();

            // Delete pre-existing host information to reload new information
            hostsFilePath += "hostInfo.txt";
            dir = new File(hostsFilePath);
            Files.deleteIfExists(dir.toPath());

            // Delete pre-existing lookup table to recreate it
            lookupTable.clearTable();

            BufferedWriter bw = new BufferedWriter(new FileWriter(hostsFilePath, true));

            // Parse `listOfHosts` string and add to text file
            String[] hostInfo = listOfHosts.split(",");

            for (int i = 0; i < hostInfo.length; i++) {
                bw.write(hostInfo[i]);
                bw.newLine();
                bw.flush();

                String[] parsedHostInfo = hostInfo[i].split(" ");
                String hostName = parsedHostInfo[0];
                lookupTable.addHost(hostName);
            }

            System.out.println("ADD METHOD: \n" + lookupTable);
            lookupTable.saveToFile("/tmp/" + LOGIN + "/linda/" + HOSTNAME + "/nets/lookupTable.txt");

            bw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Broadcasts a request to all hosts to retrieve the rawTuple and delete it in its respective host
     * @param rawTuple
     */
    private void in(String rawTuple) {
        request(rawTuple, "in");
    }


    /**
     * Broadcasts a request to all hosts to retrieve the rawTuple
     * @param rawTuple
     */
    private void rd(String rawTuple) {
        request(rawTuple, "rd");
    }


    /**
     * Places a tuple into the Tuple Space
     * @param rawTuple
     */
    private void out(String rawTuple) {
        String hostInfo = null;

        // Instantiate the tuple object
        // TODO: Create a method to check if it is a valid tuple?
        // TODO: Before adding to the queue, check to see if this tuple is equal to the tuples in our arraylist
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
            os.write(outputMessage.getBytes());
            os.close();

            s.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Removes the host from our `hostInfo.txt` file on disk, and also our lookup table
     * @param hostNames
     */
    private void delete(String hostNames) {
        String hostsFilePath = "/tmp/" + LOGIN + "/linda/" + HOSTNAME + "/nets/hostInfo.txt";
        String tempFilePath = "/tmp/" + LOGIN + "/linda/" + HOSTNAME + "/nets/temp.txt";
        ArrayList<String> hostsToRemove_ipAddr = new ArrayList<>();
        ArrayList<String> hostsToRemove_portNum = new ArrayList<>();

        // Search for the correct host name in nets file
        hostNames.replace(" ", "");
        String[] hostsToRemove = hostNames.split(",");

        for (String s: hostsToRemove) {
            // Update our locally kept list of hosts
            listOfHosts = "";

            try {
                BufferedReader reader = new BufferedReader(new FileReader(hostsFilePath));

                // Search for host information given the host name
                String hostInfo = reader.readLine();
                while (hostInfo != null) {
                    // When we find the line the host name is on, delete that line in `hostInfo.txt`
                    if (hostInfo.contains(s)) {
                        String lineToRemove = hostInfo;
                        deleteLine(lineToRemove, hostsFilePath, tempFilePath);

                        // Add info on the hosts we want to remove concerning their IP and Port number
                        String specificHostInfo[] = hostInfo.split(" ");
                        hostsToRemove_ipAddr.add(specificHostInfo[1]);
                        hostsToRemove_portNum.add(specificHostInfo[2]);
                    } else {
                        listOfHosts += hostInfo + ",";
                    }
                    hostInfo = reader.readLine();
                }

                // Removes the host from the lookup table
                lookupTable.removeHost(s);
                lookupTable.saveToFile("/tmp/" + LOGIN + "/linda/" + HOSTNAME + "/nets/lookupTable.txt");

                // Broadcast to all other nodes in the net to update their network info and lookup table info
                broadcastUpdateNetsAndLookup();

                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Send a message to all nodes who need to be deleted
        for (int i = 0; i < hostsToRemove_ipAddr.size(); i++) {
            try {
                Socket s = new Socket();
                s.connect(new InetSocketAddress(hostsToRemove_ipAddr.get(i),
                        Integer.parseInt(hostsToRemove_portNum.get(i))));

                // Preparing to send necessary information to hosts that need to be deleted
                String deleteMessage = "delete~" + listOfHosts + "~" + lookupTable.toParseableString();

                // Sending the string `listOfHosts` and `lookupTableString` to all individual hosts
                OutputStream os = s.getOutputStream();
                os.write(deleteMessage.getBytes());
                os.close();

                s.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // TODO: Need to clear `hostInfo.txt` and `lookupTable.txt` and `tuples.txt` on the host we're deleting
    }



/************************************************** Parsing Input *****************************************************/
    /**
     * This method is run on the Linda thread and determines which method to message based on the the user provided
     * input on the command line
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

                // Broadcast the updated "hostInfo.txt" and "lookupTable" to all other hosts
                broadcastUpdateNetsAndLookup();
            }

            // "out" command was inputted in Linda
            else if (parsedCommand.group(0).equalsIgnoreCase("out")) {
                if (parsedCommand.find()) {
                    out(command.substring(parsedCommand.start(), parsedCommand.end()));
                }
            }

            // "in" command was inputted in Linda
            else if (parsedCommand.group(0).equalsIgnoreCase("in")) {
                if (parsedCommand.find()) {
                    in(command.substring(parsedCommand.start(), parsedCommand.end()));
                }
            }

            // "rd" command was inputted in Linda
            else if (parsedCommand.group(0).equalsIgnoreCase("rd")) {
                if (parsedCommand.find()) {
                    rd(command.substring(parsedCommand.start(), parsedCommand.end()));
                }
            }

            // "delete" command was inputted in Linda
            else if (parsedCommand.group(0).equalsIgnoreCase("delete")) {
                if (parsedCommand.find()) {
                    delete(command.substring(parsedCommand.start(), parsedCommand.end()));
                }
            }

        }
    }


    /**
     * This method is run on the Listener thread and determines which method to message based on the data that other
     * hosts sent it
     * @param command
     */
    private void parseDataStreamCommand(String command) {
        // Parse the DataStream Command
        String[] parsedCommand = command.split("~");

        if (parsedCommand[0].equals("add")) {
            // Add Hosts
            listOfHosts = parsedCommand[1];
            add();
            System.out.print("A Connection has been established! ");

            // TODO: LOOK INTO THIS WHEN WE START DOING THE DELETE FUNCTION
            // Update Lookup Tables
//            lookupTable.updateLookupTable(parsedCommand[3]);
        }

        else if (parsedCommand[0].equals("out")) {
            String rawStringTuple = parsedCommand[1];

            try {
                // TODO: Check to see if this tuple is in the tuple array

                // Open a writer to the tuples file on this host
                String tupleSpaceFilePath = "/tmp/" + LOGIN + "/linda/" + HOSTNAME + "/tuples/tuples.txt";
                BufferedWriter bw = new BufferedWriter(new FileWriter(tupleSpaceFilePath, true));

                // Write the tuple into the host's file
                bw.write(rawStringTuple);
                bw.newLine();
                bw.flush();
                bw.close();

                System.out.print("Tuple has been added to this host: (" + rawStringTuple + ") ");
            } catch(IOException e) {
                e.printStackTrace();
            }
        }

        else if (parsedCommand[0].equals("rd") || parsedCommand[0].equals("in")) {
            String rawStringTuple = parsedCommand[1];
            String requesterIPAddr = parsedCommand[2];
            int requesterPortNum = Integer.parseInt(parsedCommand[3]);

            System.out.print("A host has requested the tuple (" + rawStringTuple + ") ");

            // Create a tuple object from the "rd" command
            Tuple tupleInSearch = new Tuple(rawStringTuple);

            // Check to see if the tuple in search is being maintained in this Tuple Space
            try {
                String tupleSpaceFilePath = "/tmp/" + LOGIN + "/linda/" + HOSTNAME + "/tuples/tuples.txt";
                File dir = new File(tupleSpaceFilePath);

                if (Files.exists(dir.toPath())) {
                    BufferedReader reader = new BufferedReader(new FileReader(tupleSpaceFilePath));

                    // Iterate through all tuples to see if there is a match
                    String stringTuple;
                    Boolean found = false;
                    while ((stringTuple = reader.readLine()) != null) {
                        Tuple tupleInFile = new Tuple(stringTuple);

                        if (tupleInSearch.equals(tupleInFile)) {
                            found = true;

                            Socket s = new Socket();
                            s.connect(new InetSocketAddress(requesterIPAddr, requesterPortNum));

                            // Sending the string `listOfHosts` to specific host
                            OutputStream os = s.getOutputStream();
                            String outputMessage = "ACK~" + rawStringTuple + "~" + HOSTNAME;
                            os.write(outputMessage.getBytes());
                            os.close();

                            s.close();

                            System.out.print("\nThe tuple (" + rawStringTuple + ") has been found in this host's " +
                                    "Tuple Space. " + "An ACK has been sent back to the requester. ");

                            // If the "in" command was invoked, we will delete the proper tuple
                            if (parsedCommand[0].equals("in")) {
                                String inputFilePath = "/tmp/" + LOGIN + "/linda/" + HOSTNAME + "/tuples/tuples.txt";
                                String tempFilePath = "/tmp/" + LOGIN + "/linda/" + HOSTNAME + "/tuples/temp.txt";
                                deleteLine(rawStringTuple, inputFilePath, tempFilePath);
                                System.out.println("\nThe tuple (" + rawStringTuple + ") has been deleted from the "
                                        + "Tuple Space. ");
                            }

                            break;
                        }
                    }

                    if (!found) {
                        requestedTuples.add(rawStringTuple);
                    }

                    reader.close();
                } else {
                    // If not found, add to the queue of tuples that are currently in search
                    requestedTuples.add(rawStringTuple);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        else if (parsedCommand[0].equals("delete")) {
            // Should return command of "delete~listofhosts~parseableStringLookupTable"
            String tuplesFilePath = "/tmp/" + LOGIN + "/linda/" + HOSTNAME + "/tuples/tuples.txt";

            // Update the `hostInfo.txt` file and lookup table
            listOfHosts = parsedCommand[1];
            add();
            lookupTable.updateLookupTable(parsedCommand[2]);

            try {
                BufferedReader reader = new BufferedReader(new FileReader(tuplesFilePath));

                // Go through file of tuples and redistribute them to their respective nodes
                String tuple = reader.readLine();
                while (tuple != null) {
                    out(tuple);
                    tuple = reader.readLine();
                }

                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        else if (parsedCommand[0].equals("ACK") && parsedCommand[1].equals(tupleThatIsBlocking)) {
            // Should return ACK of "ACK~rawTuple~hostName"
            System.out.println("\nACK Received! " + parsedCommand[2] + " maintains the tuple:(" + parsedCommand[1] + ")");
            unblockThread();
        }

        else if (parsedCommand[0].equals("ACK") && !parsedCommand[1].equals(tupleThatIsBlocking)) {
            // TODO: WE ARE NO LONGER LOOKING FOR THIS TUPLE
        }
    }



/******************************************* Miscellaneous Helper Methods *********************************************/
    /**
     * Makes sure that all hosts in the network have the same `hostInfo.txt` file in the nets directory.
     */
    private void broadcastUpdateNetsAndLookup() {
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

            if (ipAddr != IP_ADDRESS) {
                try {
                    Socket s = new Socket();
                    s.connect(new InetSocketAddress(ipAddr, portNum));

                    // Preparing to send the host information to all individual hosts
                    String hostInfoMessage = "add~" + listOfHosts;

                    // Preparing to send the lookup table to all individual hosts
                    String lookupTableMessage = "~lookupTable~";
                    lookupTableMessage += lookupTable.toParseableString();

                    // Sending the string `listOfHosts` and `lookupTableString` to all individual hosts
                    OutputStream os = s.getOutputStream();
                    os.write(hostInfoMessage.getBytes());
                    os.write(lookupTableMessage.getBytes());
                    os.close();

                    s.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * Handles the `in()` and `rd()` Linda commands
     * @param rawTuple
     * @param requestType
     */
    private void request(String rawTuple, String requestType) {
        String hostInfo = "";

        try {
            String hostsFilePath = "/tmp/" + LOGIN + "/linda/" + HOSTNAME + "/nets/hostInfo.txt";
            BufferedReader reader = new BufferedReader(new FileReader(hostsFilePath));

            // Broadcast new tuple to all hosts in the network
            while ((hostInfo = reader.readLine()) != null) {
                // Parse information from `nets` file about each host
                String[] specificHostInfo = hostInfo.split(" ");
                String ipAddr = specificHostInfo[1];
                int portNum = Integer.parseInt(specificHostInfo[2]);

                // Create a socket connection to the correct host
                Socket s = new Socket();
                s.connect(new InetSocketAddress(ipAddr, portNum));

                // Send a message in the datastream to write to the TupleSpace
                OutputStream os = s.getOutputStream();
                if (requestType.equals("rd")) {
                    String outputMessage = "rd~" + rawTuple + "~" + IP_ADDRESS + "~" + PORT_NUMBER;
                    os.write(outputMessage.getBytes());
                } else if (requestType.equals("in")) {
                    String outputMessage = "in~" + rawTuple + "~" + IP_ADDRESS + "~" + PORT_NUMBER;
                    os.write(outputMessage.getBytes());
                }
                os.close();

                s.close();
            }
            reader.close();

            // Block Linda thread & wait for an ACKnowledgement
            blockThread(rawTuple);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Utilizes the MD5 Hash to retrieve the index of the host who should hold the parameter `rawTuple`
     * @param rawTuple
     * @return
     */
    private int getHostID(String rawTuple) {
        String hashedTuple = hash(rawTuple);
        int hostID = hexToDecimal(hashedTuple);

        hostID %= lookupTable.getSIZE();
        hostID = lookupTable.findHost(hostID);

        return hostID;
    }


    /**
     * Blocks the current thread that it is called un
     * @param rawTuple
     */
    private void blockThread(String rawTuple) {
        blocked = true;
        tupleThatIsBlocking = rawTuple;
        while (true) {
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (Exception e) {
                continue;
            }
            if (!blocked) {
                break;
            }
        }
    }


    /**
     * Unblocks the thread that it is currently called on
     */
    private void unblockThread() {
        blocked = false;
        tupleThatIsBlocking = null;
    }


    /**
     * Deletes a line from `tuples.txt` that corresponds to the String passed as a parameter
     * @param lineToRemove
     */
    private void deleteLine(String lineToRemove, String inputFilePath, String tempFilePath) {
        File inputFile = new File(inputFilePath);
        File tempFile = new File(tempFilePath);

        try {
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

            String currentLine;

            while ((currentLine = reader.readLine()) != null) {
                // trim newline when comparing with lineToRemove
                String trimmedLine = currentLine.trim();
                if (trimmedLine.equals(lineToRemove)) continue;
                writer.write(currentLine + System.getProperty("line.separator"));
            }
            writer.close();
            reader.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        tempFile.renameTo(inputFile);
    }


    /**
     * Helper method to turn hex to decimal
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
     * This method listens and handles any of the data streamed from other hosts
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

        // TODO: Will need to check if we are allowed to replace on start up
        // Replace old tuple space if it exists
        try {
            String tupleSpaceFilePath = "/tmp/" + LOGIN + "/linda/" + HOSTNAME + "/tuples/";
            File dir = new File(tupleSpaceFilePath);
            Boolean success = dir.mkdirs();
            tupleSpaceFilePath += "tuples.txt";
            dir = new File(tupleSpaceFilePath);
            Files.deleteIfExists(dir.toPath());
        } catch(IOException e) {
            e.printStackTrace();
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

            // Instantiate the Lookup Table
            lookupTable = new LookupTable();

            // Get & display IP of the current machine
            IP_ADDRESS = InetAddress.getLocalHost().getHostAddress();
            System.out.println(IP_ADDRESS + " at port number: " + PORT_NUMBER);

            // Add this host to the list of hosts in our network
            listOfHosts += (HOSTNAME + " " + IP_ADDRESS + " " + PORT_NUMBER + ",");
            add();

            // Create a new thread to accept Linda Terminal Commands
            lindaTerminal = new Thread(new P2());
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * The Driver Method
     * @param args
     */
    public static void main(String[] args) {
        P2 host = new P2();
        host.listener(args);
    }
}
