import java.io.*;
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

//----------------------------------------------- Linda Commands -----------------------------------------------------//
    /**
     * Appends all host names, ip addresses, and ports in the network to the file hostInfo.txt. All information is
     * updated from the local variable `listOfHosts`
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

            // Delete pre-existing host information to load new information
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
        String hostName = specificHostInfo[0];
        String ipAddr = specificHostInfo[1];
        int portNum = Integer.parseInt(specificHostInfo[2]);

        String outputMessage = "out~" + rawTuple;
        try {
            sendDatastreamMessage(ipAddr, portNum, outputMessage);
        } catch (Exception e) {
            backupOut(rawTuple, hostName);
        }
        System.out.println("put tuple (" + rawTuple + ") on " + ipAddr);
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

                        try {
                            TimeUnit.SECONDS.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

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
                if (!listOfHosts.equals("")) {
                    broadcastUpdateNetsAndLookup();
                }

                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Send a message to all nodes who need to be deleted
        for (int i = 0; i < hostsToRemove_ipAddr.size(); i++) {
            String deleteMessage = "delete~" + listOfHosts + "~" + lookupTable.toParseableString() + "~" + HOSTNAME;
            try {
                sendDatastreamMessage(hostsToRemove_ipAddr.get((i)), Integer.parseInt(hostsToRemove_portNum.get(i)),
                        deleteMessage);
            } catch (Exception e) {
                System.out.println("You cannot delete a node that is currently unavailable");
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
        String hostName = "";

        try {
            String hostsFilePath = "/tmp/" + LOGIN + "/linda/" + HOSTNAME + "/nets/hostInfo.txt";
            BufferedReader reader = new BufferedReader(new FileReader(hostsFilePath));

            // Check to see if the tuple requested has a random variable
            if (rawTuple.contains("?")) {
                // Broadcast new tuple to all hosts in the network
                while ((hostInfo = reader.readLine()) != null) {
                    // Parse information from `nets` file about each host
                    String[] specificHostInfo = hostInfo.split(" ");
                    hostName = specificHostInfo[0];
                    String ipAddr = specificHostInfo[1];
                    int portNum = Integer.parseInt(specificHostInfo[2]);
                    String outputMessage = "";

                    if (requestType.equals("rd")) {
                        outputMessage = "rd~" + rawTuple + "~" + IP_ADDRESS + "~" + PORT_NUMBER + "~false";
                    } else if (requestType.equals("in")) {
                        outputMessage = "in~" + rawTuple + "~" + IP_ADDRESS + "~" + PORT_NUMBER + "~false";
                    }

                    try {
                        sendDatastreamMessage(ipAddr, portNum, outputMessage);
                    } catch (Exception e) {
                        System.out.println(hostName + " was down when broadcasting to find tuple. " +
                                "Making backup request");
                        backupRequest(hostName, rawTuple, requestType);
                        continue;
                    }
                }
            } else {
                // Goes to the line in the text file of the correct host
                int hostID = getHostID(rawTuple);
                for (int i = 0; i < hostID; i++) {
                    reader.readLine();
                }
                hostInfo = reader.readLine();
                reader.close();

                // Parse information about the host
                String[] specificHostInfo = hostInfo.split(" ");
                hostName = specificHostInfo[0];
                String ipAddr = specificHostInfo[1];
                int portNum = Integer.parseInt(specificHostInfo[2]);
                String outputMessage = "";

                if (requestType.equals("rd")) {
                    outputMessage = "rd~" + rawTuple + "~" + IP_ADDRESS + "~" + PORT_NUMBER + "~false";
                } else if (requestType.equals("in")) {
                    outputMessage = "in~" + rawTuple + "~" + IP_ADDRESS + "~" + PORT_NUMBER + "~false";
                }

                try {
                    sendDatastreamMessage(ipAddr, portNum, outputMessage);
                } catch (Exception e) {
                    System.out.println(hostName + " should have had the tuple, but has crashed. " +
                            "Making request to backup");
                    backupRequest(hostName, rawTuple, requestType);
                }
            }
            reader.close();

            // Block Linda thread & wait for an ACKnowledgement
            blockThread(rawTuple);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * This method saves tuples to the crashed host's backup file while the original host is unavailable
     * @param rawTuple a single tuple as a String object
     * @param crashedHostName name of the host that has crashed
     */
    private void backupOut(String rawTuple, String crashedHostName) {
        String backupHostInfo = getBackupHostInfo(crashedHostName);
        String[] specificBackupHostInfo = backupHostInfo.split(" ");
        String backupHostIpAddr = specificBackupHostInfo[1];
        int backupHostPortNum = Integer.parseInt(specificBackupHostInfo[2]);

        String outputMessage = "backup~" + rawTuple + "~true";
        try {
            sendDatastreamMessage(backupHostIpAddr, backupHostPortNum, outputMessage);
        } catch (Exception e) {
            System.out.println("The process was unable to connect with the original host and the backup host. Please" +
                    "check to make sure you have not killed two processes.");
        }
        System.out.println("put tuple (" + rawTuple + ") on " + backupHostIpAddr);
    }


    /**
     *
     * @param rawTuple
     * @param requestType
     */
    private void backupRequest(String crashedHostName, String rawTuple, String requestType) {
        String backupHostInfo = getBackupHostInfo(crashedHostName);

        // Parse information about the host
        String[] specificHostInfo = backupHostInfo.split(" ");
        String ipAddr = specificHostInfo[1];
        int portNum = Integer.parseInt(specificHostInfo[2]);
        String outputMessage = "";

        if (requestType.equals("rd")) {
            outputMessage = "rd~" + rawTuple + "~" + IP_ADDRESS + "~" + PORT_NUMBER + "~true";
        } else if (requestType.equals("in")) {
            outputMessage = "in~" + rawTuple + "~" + IP_ADDRESS + "~" + PORT_NUMBER + "~true";
        }

        try {
            sendDatastreamMessage(ipAddr, portNum, outputMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


//------------------------------------------------ Parsing Input -----------------------------------------------------//
    /**
     * This method is run on the Linda thread and determines which method to message based on the the user provided
     * input on the command line
     * @param command
     */
    private void parseLindaCommand(String command) {
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

                // Back up the new tuples that have been added to this host
                sendTupleSpaceToBackup();
            } catch(IOException e) {
                e.printStackTrace();
            }
        }

        else if (parsedCommand[0].equals("rd") || parsedCommand[0].equals("in")) {
            // Passed the command of "rd or in~tuple~requesterIpAddr~requesterPortNum~requestingFromBackup"
            String rawStringTuple = parsedCommand[1];
            String requesterIPAddr = parsedCommand[2];
            int requesterPortNum = Integer.parseInt(parsedCommand[3]);
            boolean requestingFromBackup = Boolean.valueOf(parsedCommand[4]);
            Tuple tupleInSearch = new Tuple(rawStringTuple);

            // Check to see if the tuple in search is being maintained in this Tuple Space
            try {
                String tupleSpaceFilePath = "";
                if (requestingFromBackup) {
                    tupleSpaceFilePath = "/tmp/" + LOGIN + "/linda/" + HOSTNAME + "/tuples/backupTuples.txt";
                } else {
                    tupleSpaceFilePath = "/tmp/" + LOGIN + "/linda/" + HOSTNAME + "/tuples/tuples.txt";
                }

                BufferedReader reader = new BufferedReader(new FileReader(tupleSpaceFilePath));

                // Iterate through all tuples to see if there is a match
                String stringTuple;
                Boolean found = false;
                while ((stringTuple = reader.readLine()) != null) {
                    Tuple tupleInFile = new Tuple(stringTuple);

                    if (tupleInSearch.equals(tupleInFile)) {
                        found = true;

                        String outputMessage = "ACK~" + rawStringTuple + "~" + IP_ADDRESS;
                        try {
                            sendDatastreamMessage(requesterIPAddr, requesterPortNum, outputMessage);
                        } catch (Exception e) {
                            System.out.println("The host requesting the tuple (" + rawStringTuple + ") has been killed");
                        }

                        // If the "in" command was invoked, we will delete the proper tuple and update the backups
                        if (parsedCommand[0].equals("in")) {
                            String tempFilePath = "/tmp/" + LOGIN + "/linda/" + HOSTNAME + "/tuples/temp.txt";
                            deleteLine(rawStringTuple, tupleSpaceFilePath, tempFilePath);
                            sendTupleSpaceToBackup();
                        }

                        break;
                    }
                }

                if (!found) {
                    requestedTuples.add(rawStringTuple);
                }

                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        else if (parsedCommand[0].equals("delete")) {
            // Passed the command of "delete~listofhosts~parseableStringLookupTable"
            String tuplesFilePath = "/tmp/" + LOGIN + "/linda/" + HOSTNAME + "/tuples/tuples.txt";

            // Redistribute the tuples as long as the delete command is not being called on the last node in the net
            if (parsedCommand.length > 1) {
                listOfHosts = parsedCommand[1];
                add();
                System.out.println("PARSE DATASTREAM: " + command);
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

            // Remove everything in `/nets/` and `/tuples/` directories
            String tupleSpaceFilePath = "/tmp/" + LOGIN + "/linda/" + HOSTNAME + "/tuples/";
            String hostInfoFilePath = "/tmp/" + LOGIN + "/linda/" + HOSTNAME + "/nets/";

            File dir = new File(tupleSpaceFilePath);
            deleteDir(dir);

            dir = new File(hostInfoFilePath);
            deleteDir(dir);

        }

        else if (parsedCommand[0].equals("backup")) {
            // Passed the command of "backup~parseableTupleSpaceString~append"
            if (parsedCommand.length == 3) {
                saveTupleToDisk(parsedCommand[1], true, Boolean.valueOf(parsedCommand[2]));
            }
        }

        else if (parsedCommand[0].equals("restore")) {
            // Passed the command of "restore~requesterHostName~requesterIpAddr~requesterPortNum"
            String requesterHostName = parsedCommand[1];
            String requesterIpAddr = parsedCommand[2];
            int requesterPortNum = Integer.parseInt(parsedCommand[3]);

            // Update the list of hosts to include the new port number of the requester
            String newListofHosts = "";
            String[] specificListofHosts = listOfHosts.split(",");

            for (String hostInfo : specificListofHosts) {
                if (hostInfo.contains(requesterHostName)) {
                    String newHostInfo = requesterHostName + " " + requesterIpAddr + " " + requesterPortNum + ",";
                    newListofHosts += newHostInfo;
                } else {
                    newListofHosts += hostInfo;
                    newListofHosts += ",";
                }
            }

            // Send back the nets file, backuptuples.txt, and lookuptable
            String restoreInfo = "restoreACK~" + newListofHosts + "~" + lookupTable.toParseableString() + "~"
                    + tupleSpaceToParseableString(true);
            try {
                sendDatastreamMessage(requesterIpAddr, requesterPortNum, restoreInfo);
            } catch (Exception e) {
                System.out.println(requesterIpAddr + " was trying to restore after a restart but has crashed again");
            }
        }

        else if (parsedCommand[0].equals("restoreACK")) {
            // Passed the command of "restoreACK~listOfHosts~lookupTableString~backupTuplesString"
            listOfHosts = parsedCommand[1];
            lookupTable.updateLookupTable(parsedCommand[2]);
            if (parsedCommand.length == 4) {
                saveTupleToDisk(parsedCommand[3], false, false);
            }
            add();
            broadcastUpdateNetsAndLookup();
        }

        else if (parsedCommand[0].equals("ACK") && parsedCommand[1].equals(tupleThatIsBlocking)) {
            // Passed the command "ACK~rawTuple~hostIpAddr"
            System.out.println("get tuple (" + parsedCommand[1] + ") on " + parsedCommand[2]);
            unblockThread();
        }

        else if (parsedCommand[0].equals("ACK") && !parsedCommand[1].equals(tupleThatIsBlocking)) {
            // TODO: WE ARE NO LONGER LOOKING FOR THIS TUPLE
        }
    }


//------------------------------------------ Miscellaneous Helper Methods --------------------------------------------//
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
     *
     */
    private void sendTupleSpaceToBackup() {
        String backupHost = getBackupHostInfo(HOSTNAME);
        String backupHostInfo[] = backupHost.split(" ");
        String backupIpAddr = backupHostInfo[1];
        int backupPortNum = Integer.parseInt(backupHostInfo[2]);

        String parseableTupleString = tupleSpaceToParseableString(false);
        String outputMessage = "backup~" + parseableTupleString + "~false";

        try {
            sendDatastreamMessage(backupIpAddr, backupPortNum, outputMessage);
        } catch (Exception e) {
            // Unable to connect with the backup host
            // TODO: When the backup host is alive again, we need to back up this tuple space
        }
    }


    /**
     * Parses the input of tuples and saves it to the backupTuples.txt file
     * @param parseableString Needs to be inputted a list of tuples separated by the ` delimiter
     */
    private void saveTupleToDisk(String parseableString, Boolean saveToBackup, Boolean append) {
        String tuplesArray[] = parseableString.split("`");
        String tupleSpaceFilePath = "";

        try {
            // Open a writer to the tuples file on this host
            if (saveToBackup) {
                tupleSpaceFilePath = "/tmp/" + LOGIN + "/linda/" + HOSTNAME + "/tuples/backupTuples.txt";
            } else {
                tupleSpaceFilePath = "/tmp/" + LOGIN + "/linda/" + HOSTNAME + "/tuples/tuples.txt";
            }

            // Delete existing backup
            if (!append) {
                try {
                    File dir = new File(tupleSpaceFilePath);
                    dir.delete();
                    dir.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // Write a new backup based on string passed
            BufferedWriter bw = new BufferedWriter(new FileWriter(tupleSpaceFilePath, true));
            for (int i = 0; i < tuplesArray.length; i++) {
                bw.write(tuplesArray[i]);
                bw.newLine();
                bw.flush();
            }
            bw.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }


    /**
     *
     */
    private void restoreFromBackup() {
        // TODO: This is assuming we do not add() or delete() a node while there is a host that has crashed
        String backupHostInfo = getBackupHostInfo(HOSTNAME);
        String specificBackupHostInfo[] = backupHostInfo.split(" ");
        String backupHostName = specificBackupHostInfo[0];
        String backupHostIpAddr = specificBackupHostInfo[1];
        int backupHostPortNum = Integer.parseInt(specificBackupHostInfo[2]);

        // Send message to backup host to "Restore"
        String message = "restore~" + HOSTNAME + "~" + IP_ADDRESS + "~" + PORT_NUMBER;
        try {
            sendDatastreamMessage(backupHostIpAddr, backupHostPortNum, message);
        } catch (Exception e) {
            System.out.println("Tried to request information from " + backupHostIpAddr + " to restore but this host" +
                    "is not currently available");
        }


        // Datastream Handler will receive info about nets file, lookup table, and tuple space. It will then broadcast
        // its new hostInfo.txt
    }


    /**
     * Returns the information of this current node's backup node. Each node's backup node is the node that is listed
     * after this one in the `hostInfo.txt` file.
     * @return
     */
    private String getBackupHostInfo(String currentHostName) {
        String hostInfo = "";
        String firstHostInfo;

        try {
            // Retrieve the information of the correct host
            String hostInfoFilePath = "/tmp/" + LOGIN + "/linda/" + HOSTNAME + "/nets/hostInfo.txt";
            BufferedReader reader = new BufferedReader(new FileReader(hostInfoFilePath));

            // Goes to the line in the text file of the correct host
            hostInfo = reader.readLine();
            firstHostInfo = hostInfo;
            while (hostInfo != null) {
                if (hostInfo.contains(currentHostName)) {
                    hostInfo = reader.readLine();
                    if (hostInfo != null) {
                        return hostInfo;
                    } else {
                        return firstHostInfo;
                    }
                }
                hostInfo = reader.readLine();
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return hostInfo;
    }


    /**
     *
     * @return List of tuples all separated by the ` delimiter
     */
    private String tupleSpaceToParseableString(boolean fromBackup) {
        String result = "";
        String tupleSpaceFilePath = "";

        if (fromBackup) {
            tupleSpaceFilePath = "/tmp/" + LOGIN + "/linda/" + HOSTNAME + "/tuples/backupTuples.txt";
        } else {
            tupleSpaceFilePath = "/tmp/" + LOGIN + "/linda/" + HOSTNAME + "/tuples/tuples.txt";
        }

        try {
            // Retrieve the information of the correct host
            BufferedReader reader = new BufferedReader(new FileReader(tupleSpaceFilePath));

            // Goes to the line in the text file of the correct host
            String tuple = reader.readLine();
            while (tuple != null) {
                result += (tuple + "`");
                tuple = reader.readLine();
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
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
     * Generic method to send a message to a certain host through the datastream
     * @param ipAddr
     * @param portNum
     * @param message
     */
    private void sendDatastreamMessage(String ipAddr, int portNum, String message) throws Exception {
        // Create a socket connection to the correct host
        Socket s = new Socket();
        s.connect(new InetSocketAddress(ipAddr, portNum));

        // Send a message in the datastream to write to the TupleSpace
        OutputStream os = s.getOutputStream();
        os.write(message.getBytes());
        os.close();

        s.close();
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
     *
     * @param file
     */
    void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteDir(f);
            }
        }
        file.delete();
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


//--------------------------------------- Methods for Linda Thread & Socket Thread -----------------------------------//
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

        // Create a new tuple space
        try {
            String tupleSpaceFilePath = "/tmp/" + LOGIN + "/linda/" + HOSTNAME + "/tuples/";
            File dir = new File(tupleSpaceFilePath);
            dir.mkdirs();

            tupleSpaceFilePath+="tuples.txt";
            dir = new File(tupleSpaceFilePath);
            dir.delete();
            dir.createNewFile();
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

            // Check to see if this host is recovering from previously crashing
            String netsFilePath = "/tmp/" + LOGIN + "/linda/" + HOSTNAME + "/nets/";
            File directory = new File(netsFilePath);
            if (directory.exists()) {
                System.out.println("Restoring from backup");
                restoreFromBackup();
            }

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
