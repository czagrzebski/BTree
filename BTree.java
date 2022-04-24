import java.io.*;
import java.util.LinkedList;
import java.util.Stack;

public class BTree {
    private RandomAccessFile f;
    private int order;
    private int blockSize;
    private long root;
    private long free;

    // add instance variables as needed.
    private class BTreeNode {
        private int count;
        private int keys[];
        private long children[];
        private long address; // the address of the node in the file
        // constructors and other method

        private BTreeNode(int count, int[] keys, long[] children){
            this.count = count;
            this.keys = keys;
            this.children = children;
        }

        // Read a BTreeNode from Memory
        private BTreeNode(long addr) throws IOException {
            this.address = addr;

            f.seek(addr);

            this.count = f.readInt();

            //Max number of keys = M - 1
            keys = new int[order - 1];

            //Max number of children is M
            //make room for the next leaf reference (+1)
            children = new long[order + 1];

            for(int i=0; i < keys.length; i++)
                keys[i] = f.readInt();

            for(int i=0; i < children.length; i++)
                children[i] = f.readLong();

        }

        private void writeNode(long addr) throws IOException{
            f.seek(addr);
            f.writeInt(this.count);

            for(int i=0; i < keys.length; i++)
                f.writeInt(keys[i]);

            for(int i=0; i < children.length; i++)
                f.writeLong(children[i]);
        }
    }

    public BTree(String filename, int bsize) throws IOException {
        // bsize is the block size. This value is used to calculate the order
        // of the B+Tree
        // all B+Tree nodes will use bsize bytes
        // makes a new B+tree

        File bFile = new File(filename);

        if(bFile.exists())
            bFile.delete();

        f = new RandomAccessFile(bFile, "rw");

        //Start at the beginning of the file
        f.seek(0);

        // Set the root and free addresses in memory and in the file
        this.root = 0;
        f.writeLong(root);

        this.free = 0;
        f.writeLong(free);

        //Set the blocksize in memory and in the file
        this.blockSize = bsize;
        f.writeInt(blockSize);

        //Calculate the order 
        this.order = bsize/12;
    }

    public BTree(String filename) {
        // open an existing B+Tree
    }

    public void testPrint() throws IOException {
        BTreeNode testNode = new BTreeNode(20);

        System.out.println(testNode.count);
        for(int i=0; i < testNode.keys.length; i++){
            System.out.println(testNode.keys[i]);
        }

        for(int i=0; i < testNode.children.length; i++){
            System.out.println(testNode.children[i]);
        }
    }


    public Stack<BTreeNode> findPath(long addr, int key) throws IOException{
        return findPathRec(addr, new Stack<BTreeNode>(), key);
    }

    private Stack<BTreeNode> findPathRec(long addr, Stack<BTreeNode> path, int key) throws IOException{
        if(addr == 0)
            return path;

        BTreeNode currNode = new BTreeNode(addr);

        path.push(currNode);

        //Leaf found
        if(currNode.count < 0)
            return path;

        for(int i=0; i < Math.abs(currNode.count); i++){
            if(key < currNode.keys[i]) {
                 findPathRec(currNode.children[i], path, key);
            }

            //End has been reached, so return last child
            if(i == Math.abs(currNode.count) - 1){
                findPathRec(currNode.children[i + 1], path, key);
            }
        }
    

        return path;
    }

    public boolean insert(int key, long addr) throws IOException {
        /*
         * If key is not a duplicate add key to the B+tree
         * addr (in DBTable) is the address of the row that contains the key
         * return true if the key is added
         * return false if the key is a duplicate
         */

        int[] keys = new int[order - 1];
        long[] children = new long[order + 1];

        //Root is empty
        if(root == 0){
            //Not implemented
            long newNodeAddr = malloc();
            keys[0] = key;
            children[0] = addr;
            BTreeNode newNode = new BTreeNode(-1, keys, children);
            root = newNodeAddr;
            newNode.writeNode(newNodeAddr);
            return true;
         
        }

        boolean split = false; 

        Stack<BTreeNode> path = findPath(root, key);

        //Should be a leaf node
        BTreeNode currNode = path.pop();

        //there is room in the node for a value
        if(Math.abs(currNode.count) < (order - 1)){
            for(int i=Math.abs(currNode.count); i > 0; i--){
                if(currNode.keys[i - 1] > key) {                
                    currNode.keys[i] = currNode.keys[i - 1];
                    currNode.keys[i - 1] = key;
    
                    currNode.children[i] = currNode.children[i - 1];
                    currNode.children[i - 1] = addr;
                } else {
                    currNode.keys[i] = key;
                    currNode.children[i] = addr;
                    break;
                }
            } 

            currNode.count -= 1;
            currNode.writeNode(currNode.address);
            split = false;
        } else {
            //Splitting a leaf node
            BTreeNode newNode;
            int[] splitKeys = new int[order - 1];
            int[] splitChildren = new int[order + 1];

            int newSize = (int) Math.ceil(currNode.count / 2);

            

            System.out.println(newSize);
        }

        return true;
    }

    public long malloc() throws IOException{
        long address = 0;

        BTreeNode freeNode;

        if(free == 0) {
            address = f.length();
        } else {
            freeNode = new BTreeNode(free);
            address = free;
            free = freeNode.children[freeNode.children.length - 1];
        }

        return address;

    }

    public long remove(int key) {
        /*
         * If the key is in the Btree, remove the key and return the address of the
         * row
         * return 0 if the key is not found in the B+tree
         */

        return 0;
    }

    public long search(int k) {
        /*
         * This is an equality search
         * If the key is found return the address of the row with the key
         * otherwise return 0
         */
        return 0;
    }

    public LinkedList<Long> rangeSearch(int low, int high) {
        // PRE: low <= high
        /*
         * return a list of row addresses for all keys in the range low to high
         * inclusive
         * return an empty list when no keys are in the range
         */

        return new LinkedList<Long>();
    }

    public void print() {
        // print the B+Tree to standard output
        // print one node per line
        // This method can be helpful for debugging
    }

    public void close() {
        // close the B+tree. The tree should not be accessed after close is called
    }
}
