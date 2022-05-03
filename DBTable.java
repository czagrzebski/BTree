import java.io.*;
import java.util.LinkedList;

public class DBTable {
    private RandomAccessFile rows; // the file that stores the rows in the table
    private BTree index;
    private long free; // head of the free list space for rows
    private int numOtherFields;
    private int otherFieldLengths[];
    // add other instance variables as needed

    private class Row {
        private int keyField;
        private char otherFields[][];
        /*
         * Each row consists of unique key and one or more character array fields.
         * Each character array field is a fixed length field (for example 10
         * characters).
         * Each field can have a different length.
         * Fields are padded with null characters so a field with a length of
         * of x characters always uses space for x characters.
         */
        // Constructors and other Row methods

        private Row(){
            
        }

        private Row(int key, char[][] otherFields){
            this.keyField = key;
            this.otherFields = otherFields;
        }

        private Row(long addr) throws IOException {
            rows.seek(addr);

            this.keyField = rows.readInt();

            otherFields = new char[numOtherFields][];

            for(int i=0; i < otherFields.length; i++){
                otherFields[i] = new char[otherFieldLengths[i]];
                for(int j=0; j < otherFields[i].length; j++){
                    otherFields[i][j] = rows.readChar();
                }
            }

        }

        private void writeNode(long addr) throws IOException {
            rows.seek(addr);

            rows.writeInt(this.keyField);

            for(int i=0; i < otherFields.length; i++){
                for(int j=0; j < otherFields[i].length; j++){
                    rows.writeChar(otherFields[i][j]);
                }
            }

        }

    }

    public DBTable(String filename, int fL[], int bsize) throws IOException{
        /*
         * Use this constructor to create a new DBTable.
         * filename is the name of the file used to store the table
         * fL is the lengths of the otherFields
         * fL.length indicates how many other fields are part of the row
         * bsize is the block size. It is used to calculate the order of the B+Tree
         * A B+Tree must be created for the key field in the table
         * 
         * If a file with name filename exists, the file should be deleted before the
         * new file is created.
         */

        File dbFile = new File(filename);
        index = new BTree(filename + ".tree", bsize);

        //Delete the file if it already exists
        if(dbFile.exists())
            dbFile.delete();

        rows = new RandomAccessFile(dbFile, "rw");

        //Start at the beginning of the file
        rows.seek(0);

        this.numOtherFields = fL.length;
        rows.writeInt(numOtherFields);

        otherFieldLengths = new int[fL.length];
        for(int i=0; i < fL.length; i++) {
            otherFieldLengths[i] = fL[i];
            rows.writeInt(fL[i]);
        }

        this.free = 0;
        rows.writeLong(0);
        


    }

    public DBTable(String filename) throws IOException {
        // Use this constructor to open an existing DBTable
        File dbFile = new File(filename);
        index = new BTree(filename + ".tree");

        rows = new RandomAccessFile(dbFile, "rw");

        rows.seek(0);

        this.numOtherFields = rows.readInt();

        this.otherFieldLengths = new int[this.numOtherFields];
        for(int i=0; i < numOtherFields; i++){
            this.otherFieldLengths[i] = rows.readInt();
        }

        this.free = rows.readLong();
    }

    public boolean insert(int key, char fields[][]) throws IOException {
        // PRE: the length of each row is fields matches the expected length
        /*
         * If a row with the key is not in the table, the row is added and the method
         * returns true otherwise the row is not added and the method returns false.
         * The method must use the B+tree to determine if a row with the key exists.
         * If the row is added the key is also added into the B+tree.
         */

         // It does not already exist
         if(index.search(key) == 0){
            Row newRow = new Row(key, fields);
            long newRowAddr = malloc();
            newRow.writeNode(newRowAddr);
            index.insert(key, newRowAddr);
            return true;
         } 

      
         // Key already exists in the table
         return false;
    }

    public void printBTree() throws IOException {
        index.print();
    }

    public long malloc() throws IOException {
        long address = 0;

        Row freeNode;

        if (free == 0) {
            address = rows.length();
        } else {
            freeNode = new Row(free);
            address = free;
            free = freeNode.keyField;
        }

        return address;

    }

    public boolean remove(int key) throws IOException {
        /*
         * If a row with the key is in the table it is removed and true is returned
         * otherwise false is returned.
         * The method must use the B+Tree to determine if a row with the key exists.
         * 
         * If the row is deleted the key must be deleted from the B+Tree
         */

         if(index.remove(key) == 0)
            return false;
        
        return true;
    }

    public LinkedList<String> search(int key) throws IOException {
    /*
     * If a row with the key is found in the table return a list of the other fields
     * in
     * the row.
     * The string values in the list should not include the null characters.
     * If a row with the key is not found return an empty list
     * The method must use the equality search in B+Tree
     */
        LinkedList<String> toReturn = new LinkedList<>();

        long dbAddress = index.search(key);

        if(dbAddress == 0)
            return toReturn;

        Row row = new Row(dbAddress);

        for(int i=0; i < row.otherFields.length; i++){
            String field = "";
            for(int j=0; j < row.otherFields[i].length; j++){
                if(row.otherFields[i][j] != 0)
                    field += row.otherFields[i][j];
            }
            toReturn.add(field);
        }


        return toReturn;
    }

    public LinkedList<LinkedList<String>> rangeSearch(int low, int high) throws IOException {
        // PRE: low <= high
        /*
         * For each row with a key that is in the range low to high inclusive a list
         * of the fields (including the key) in the row is added to the list
         * returned by the call.
         * If there are no rows with a key in the range return an empty list
         * The method must use the range search in B+Tree
         */
        LinkedList<LinkedList<String>> toReturn = new LinkedList<>();

        LinkedList<Long> addressesInRange = index.rangeSearch(low, high);

        for(long address: addressesInRange){
            Row row = new Row(address);
            LinkedList<String> rowData = new LinkedList<>();
            rowData.add(String.valueOf(row.keyField));
            for(int i=0; i < row.otherFields.length; i++){
                String field = "";
                for(int j=0; j < row.otherFields[i].length; j++){
                    if(row.otherFields[i][j] != 0)
                        field += row.otherFields[i][j];
                }
                rowData.add(field);
            }

            toReturn.add(rowData);
        }

        return toReturn;
    }

    public void printFreeBTree() throws IOException {
        index.printFreeList();
    }

    public void print() throws IOException {
        // Print the rows to standard output is ascending order (based on the keys)
        // One row per line
        LinkedList<LinkedList<String>> data = this.rangeSearch(Integer.MIN_VALUE, Integer.MAX_VALUE);

        // Print the table header
          System.out.format("%-8s", "Key");
          for (int i = 0; i < numOtherFields; i++) {
              System.out.format(" %-" + (otherFieldLengths[i]) + "s ", "Field " + (i + 1));
          }


        System.out.println();
        for(LinkedList<String> row:data){
              // Print out each field and calculate the padding automatically
            System.out.format("%-8s", row.pop());
            for (int i = 0; i < numOtherFields; i++) {
                System.out.format(" %-" + (otherFieldLengths[i]) + "s ", row.pop());
            }
            System.out.println();
        }
    }

    public void free(){
        
    }

    public void close() throws IOException {
        // close the DBTable. The table should not be used after it is closed
        rows.seek(0);
        rows.writeInt(numOtherFields);
        for(int i=0; i < otherFieldLengths.length; i++){
            rows.writeInt(otherFieldLengths[i]);
        }

        rows.writeLong(this.free);

        index.close();
        
    }
}
