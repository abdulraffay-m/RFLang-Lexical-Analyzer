import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SymbolTable.java
 * Stores identifier information: name, type, first occurrence (line:col), and frequency.
 * Uses a LinkedHashMap to preserve insertion order for display.
 *
 * CS4031 - Compiler Construction - Assignment 01
 * Abdul Raffay (23i-0587) & Ibrahim Azad (23i-3049)
 */
public class SymbolTable {

    public static class Entry {
        private String name;
        private String type;
        private int firstLine;
        private int firstColumn;
        private int frequency;

        public Entry(String name, String type, int firstLine, int firstColumn) {
            this.name = name;
            this.type = type;
            this.firstLine = firstLine;
            this.firstColumn = firstColumn;
            this.frequency = 1;
        }

        public void incrementFrequency() { frequency++; }
        public String getName()        { return name; }
        public String getType()        { return type; }
        public int getFirstLine()      { return firstLine; }
        public int getFirstColumn()    { return firstColumn; }
        public int getFrequency()      { return frequency; }

        @Override
        public String toString() {
            return "Name: " + name + ", Type: " + type
                 + ", First Occurrence: Line " + firstLine + " Col " + firstColumn
                 + ", Frequency: " + frequency;
        }
    }

    private LinkedHashMap<String, Entry> table;

    public SymbolTable() {
        table = new LinkedHashMap<String, Entry>();
    }

    public void addIdentifier(String name, int line, int column) {
        if (table.containsKey(name)) {
            table.get(name).incrementFrequency();
        } else {
            table.put(name, new Entry(name, "IDENTIFIER", line, column));
        }
    }

    public Entry lookup(String name) { return table.get(name); }
    public int size() { return table.size(); }

    public void display() {
        System.out.println("\n========================================");
        System.out.println("         SYMBOL TABLE");
        System.out.println("========================================");
        if (table.isEmpty()) {
            System.out.println("  (empty)");
        } else {
            System.out.printf("  %-25s %-12s %-20s %-10s%n", "Name", "Type", "First Occurrence", "Frequency");
            System.out.println("  " + "-------------------------------------------------------------------");
            for (Entry entry : table.values()) {
                System.out.printf("  %-25s %-12s Line %-3d Col %-8d %-10d%n",
                    entry.getName(), entry.getType(),
                    entry.getFirstLine(), entry.getFirstColumn(), entry.getFrequency());
            }
        }
        System.out.println("========================================");
        System.out.println("  Total unique identifiers: " + table.size());
        System.out.println("========================================\n");
    }

    public Map<String, Entry> getEntries() { return table; }
}