import java.util.ArrayList;


public class Tuple {
    private ArrayList<String> tuple;
    private ArrayList<String> tupleType;

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
                tupleType.add("integer");
            }
        }
    }

    private int getSize() {
        return tuple.size();
    }

    public String getTupleElement(int index) {
        return tuple.get(index);
    }

    public String getTupleTypeElement(int index) {
        return tupleType.get(index);
    }

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
                if (!this.getTupleElement(i).equals(parsedVariable[1])) {
                    return false;
                }
            }
            else if(this.getTupleTypeElement(i).equals("variable")){
                String variable = this.getTupleElement(i);
                String[] parsedVariable = variable.split(":");
                if (!tuple.getTupleElement(i).equals(parsedVariable[1])) {
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