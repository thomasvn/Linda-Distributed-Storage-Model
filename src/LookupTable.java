import java.util.ArrayList;

/**
 * Partition the Lookup Table based on the number of hosts added
 */
public class LookupTable {
    private ArrayList<String> lookupTable_hosts;
    private ArrayList<ArrayList<Range>> lookupTable_ranges;
    private int SIZE;

    public LookupTable (int size) {
        this.SIZE = size;
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

    // TODO: Save the look up file to disk (pass the file path)

    @Override
    public String toString() {
        String lookupTable = "";
        for (int i = 0; i < lookupTable_hosts.size(); i++) {
            lookupTable += ("Host: " + lookupTable_hosts.get(i) + "  ");

            ArrayList<Range> singleHostRanges = lookupTable_ranges.get(i);
            for (Range r: singleHostRanges) {
                lookupTable += ("Min: " + r.getMin() + " Max: " + r.getMax() + "  ");
            }

            if (i != lookupTable_hosts.size() - 1) {
                lookupTable += "\n";
            }
        }
        return lookupTable;
    }

    public static void main(String[] args) {
        LookupTable lookupTable = new LookupTable(100);
        lookupTable.addHost("h1");
        lookupTable.addHost("h2");
        lookupTable.addHost("h3");
        lookupTable.addHost("h4");
        System.out.println(lookupTable);
        System.out.println();
        lookupTable.removeHost("h3");
        System.out.println(lookupTable);
    }
}
