import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

/**
 * Partition the Lookup Table based on the number of hosts added
 */
public class LookupTable {
    private ArrayList<String> lookupTable_hosts;
    private ArrayList<ArrayList<Range>> lookupTable_ranges;
    private int SIZE = 100; // TODO: Change to 2^16

    public LookupTable () {
        lookupTable_hosts = new ArrayList<>();
        lookupTable_ranges = new ArrayList<>();
    }


    public void addHost(String hostName) {
        ArrayList<Range> hostRange = new ArrayList<>();

        // Run this code block if the lookup table is currently empty
        if (lookupTable_hosts.isEmpty()) {
            hostRange.add(new Range(0, this.SIZE));
            lookupTable_ranges.add(hostRange);
            lookupTable_hosts.add(hostName);
            return;
        }

        // Inspect all hosts currently available in the lookup table
        int numHosts = lookupTable_hosts.size();
        for (int i = 0; i < numHosts; i++) {
            // Inspect each individual host's list of ranges
            ArrayList<Range> singleHostRanges = lookupTable_ranges.get(i);
            for (Range r: singleHostRanges) {
                int numIndicesNeeded = (r.getMax() - r.getMin()) / (numHosts + 1);

                // Trim the maximum of the older host's range for the new host
                hostRange.add(new Range(r.getMax() - numIndicesNeeded, r.getMax()));
                r.setMax(r.getMax() - numIndicesNeeded);
            }
        }

        lookupTable_ranges.add(hostRange);
        lookupTable_hosts.add(hostName);
    }


    public void removeHost(String hostName) {
        int numRemainingHosts = lookupTable_hosts.size() - 1;
        ArrayList<Range> redistributedRanges = new ArrayList<>();
        ArrayList<Range> hostRange;

        // Find index of host in the lookup table
        int hostIndex = lookupTable_hosts.indexOf(hostName);
        if (hostIndex == -1) {
            System.out.println("Host was not found in the Lookup Table");
        }

        // Use this index to refer to the list of ranges
        hostRange = lookupTable_ranges.get(hostIndex);

        // Divide each of the ranges into "numHosts" number of sub-ranges
        for (Range r: hostRange) {
            int numIndicesNeeded = (r.getMax() - r.getMin()) / numRemainingHosts;
            int min = r.getMin();
            for (int i = 0; i < numRemainingHosts; i++) {
                // Add all these sub-ranges to the `redistributedRanges` array
                redistributedRanges.add(new Range(min, min + numIndicesNeeded));
                min += numIndicesNeeded;
            }
        }

        // Remove from both Arraylists which represent lookup table
        lookupTable_hosts.remove(hostIndex);
        lookupTable_ranges.remove(hostIndex);

        // Assign these redistributed ranges to their respective hosts
        for (int i = 0; i < redistributedRanges.size(); i++) {
            ArrayList<Range> originalRange = lookupTable_ranges.get(i % lookupTable_hosts.size());
            originalRange.add(redistributedRanges.get(i));
            lookupTable_ranges.set(i % lookupTable_hosts.size(), originalRange);
        }
    }


    /**
     * Given a range number, it returns which host utilizes that range number.
     * This method will only be messaged in `P2.java` inside the method `getHostID()`
     * @param rangeNum
     * @return
     */
    public int findHost(int rangeNum) {
        int indexOfHost = 0;
        for (int i = 0; i < lookupTable_ranges.size(); i++) {
            ArrayList<Range> hostRanges = lookupTable_ranges.get(i);
            for (Range r: hostRanges) {
                if (r.withinRange(rangeNum)) {
                    indexOfHost = i;
                }
            }
        }
        return indexOfHost;
    }


    /**
     * @return Format: "h1:0,26;`h2:50,76;`h3:34,46;84,96;`h4:26,34;76,84;46,50;96,100;42,46;92,96;"
     */
    public String toParseableString() {
        String result = "";

        for (int i = 0; i < lookupTable_hosts.size(); i++) {
            result += lookupTable_hosts.get(i) + ":";

            ArrayList<Range> hostRanges = lookupTable_ranges.get(i);
            for (Range r: hostRanges) {
                result += r.getMin() + "," + r.getMax() + ";";
            }

            result += "`";
        }

        return result;
    }


    /**
     * Update the current state of this object's lookup table based on the parseable string that was passed ins
     * @param parseableLookupTable Format: "h1:0,26;`h2:50,76;`h3:34,46;84,96;`h4:26,34;76,84;46,50;96,100;42,46;92,96;"
     */
    public void updateLookupTable(String parseableLookupTable) {
        // Delete the data in the current lookup table
        lookupTable_hosts.clear();
        lookupTable_ranges.clear();

        // Organize by each individual host and its ranges
        String[] hostsAndRanges = parseableLookupTable.split("`");

        // Update the lookup table based on each of these hosts' information
        for(String s: hostsAndRanges) {
            String[] hostAndRanges = s.split(":");
            String hostName = hostAndRanges[0];
            String hostRanges = hostAndRanges[1];

            lookupTable_hosts.add(hostName);

            // Iterate through the ranges and add them to the lookup table
            ArrayList<Range> newRanges = new ArrayList<>();
            String[] ranges = hostRanges.split(";");
            for (String r: ranges) {
                String[] minAndMax = r.split(",");
                int min = Integer.parseInt(minAndMax[0]);
                int max = Integer.parseInt(minAndMax[1]);

                newRanges.add(new Range(min, max));
            }
            lookupTable_ranges.add(newRanges);
        }
    }


    /**
     *
     */
    public void clearTable() {
        lookupTable_hosts.clear();
        lookupTable_ranges.clear();
    }


    /**
     * Writes the lookupTable to /tmp/LOGIN/linda/HOST/nets/lookupTable.txt using the current state of the lookup table
     * @param filePath
     */
    public void saveToFile(String filePath) {
        // Create necessary file paths for the lookup table
        File dir = new File(filePath);

        try {
            // Delete pre-existing host information to reload new information
            Files.deleteIfExists(dir.toPath());

            BufferedWriter bw = new BufferedWriter(new FileWriter(filePath, true));


            for (int i = 0; i < lookupTable_hosts.size(); i++) {
                bw.write(lookupTable_hosts.get(i) + ": ");

                ArrayList<Range> hostRanges = lookupTable_ranges.get(i);
                for (Range r: hostRanges) {
                    bw.write("(" + r.getMin() + "," + r.getMax() + ")");
                }

                bw.newLine();
                bw.flush();
            }

            bw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public int getSIZE() {
        return this.SIZE;
    }


    @Override
    public String toString() {
        String lookupTable = "";
        for (int i = 0; i < lookupTable_hosts.size(); i++) {
            lookupTable += ("Host: " + lookupTable_hosts.get(i) + "  ");

            ArrayList<Range> singleHostRanges = lookupTable_ranges.get(i);
            for (Range r: singleHostRanges) {
                lookupTable += ("(" + r.getMin() + ", " + r.getMax() + ")  ");
            }

            if (i != lookupTable_hosts.size() - 1) {
                lookupTable += "\n";
            }
        }
        return lookupTable;
    }


    public static void main(String[] args) {
        LookupTable lookupTable = new LookupTable();
        lookupTable.addHost("h1");
        lookupTable.addHost("h2");
        lookupTable.addHost("h3");
        lookupTable.addHost("h4");
        System.out.println(lookupTable);
        System.out.println();
        lookupTable.removeHost("h3");
        System.out.println(lookupTable);
        System.out.println();

        System.out.println("90 belongs to: " + lookupTable.findHost(90));
        System.out.println("80 belongs to: " + lookupTable.findHost(80));
        System.out.println("70 belongs to: " + lookupTable.findHost(70));
        System.out.println("60 belongs to: " + lookupTable.findHost(60));
        System.out.println("50 belongs to: " + lookupTable.findHost(50));

//        String filePath = "/tmp/tnguyen1/linda/h1/nets/lookupTable.txt";
//        lookupTable.saveToFile(filePath);

        System.out.println();
        String parseableString = lookupTable.toParseableString();
        System.out.println(parseableString);
        lookupTable.updateLookupTable(parseableString);
        System.out.println(lookupTable);
    }
}
