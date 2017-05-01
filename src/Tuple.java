import java.util.ArrayList;

/**
 * This class represents a single tuple and is able to maintain / parse the tuple
 */
public class Tuple {
    private ArrayList<String> tuple;
    private ArrayList<String> tupleType;


    /**
     * Constructor method for the tuple. Also analyzes the contents of the tuple and places it into an arraylist which
     * corresponds to the type of the tuple.
     *
     * @param tupleString should be passed the tuple without the delimiters "(" and ")"
     */
    public Tuple(String tupleString) {
        tuple = new ArrayList<>();
        tupleType = new ArrayList<>();

        String[] split = tupleString.split(",");

        for (String s : split) {
            tuple.add(s);

            if (s.contains(".")) {
                tupleType.add("float");
            }
            else if (s.contains("\"")) {
                tupleType.add("string");
            }
            else if (s.contains("?")) {
                tupleType.add("variable");
            }
            else {
                tupleType.add("int");
            }
        }
    }


    /**
     * Returns the size of the tuple
     *
     * @return integer size of tuple
     */
    private int getSize() {
        return tuple.size();
    }


    /**
     * Gets a specific element inside the tuple
     *
     * @param index represents the index of the element in the tuple
     * @return string representation of the element
     */
    public String getTupleElement(int index) {
        return tuple.get(index);
    }


    /**
     * Gets a specific element inside the arraylist of tuple types
     *
     * @param index represents the index of the element in the tuple
     * @return string that identifies the type of the tuple element
     */
    public String getTupleTypeElement(int index) {
        return tupleType.get(index);
    }


    /**
     * Checks to see if two tuples are equal to one another.
     *
     * If the tuple does not contain a variable, it will check to see if the elements are an exact match
     *
     * If the tuple does contain a variable, it will check to see if the element types match for the tuple element that
     * contains the variable
     *
     * @param tuple must be passed a tuple object
     * @return boolean value which represents whether the two tuple objects are equal or not
     */
    public boolean equals(Tuple tuple) {
        // Easy check to see if tuples are not the same
        if (tuple.getSize() != this.getSize()) {
            return false;
        }

        for (int i = 0; i < this.getSize(); i++) {
            // Check to see if variable type matches with other tuple's type
            if (tuple.getTupleTypeElement(i).equals("variable")) {
                String variable = tuple.getTupleElement(i);
                String[] parsedVariable = variable.split(":");
                if (!this.getTupleTypeElement(i).equalsIgnoreCase(parsedVariable[1])) {
                    return false;
                }
            }
            else if(this.getTupleTypeElement(i).equals("variable")){
                String variable = this.getTupleElement(i);
                String[] parsedVariable = variable.split(":");
                if (!tuple.getTupleTypeElement(i).equalsIgnoreCase(parsedVariable[1])) {
                    return false;
                }
            }
            else if (!this.getTupleElement(i).equals(tuple.getTupleElement(i))) {
                return false;
            }
        }
        return true;
    }
}