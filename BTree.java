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

        private BTreeNode() {
            keys = new int[order];
            children = new long[order + 1];
            count = 0;
        }

        private BTreeNode(int count, int[] keys, long[] children) {
            this.count = count;
            this.keys = keys;
            this.children = children;
        }

        // Read a BTreeNode from Memory
        private BTreeNode(long addr) throws IOException {
            this.address = addr;

            f.seek(addr);

            this.count = f.readInt();

            // Max number of keys = M - 1
            keys = new int[order];

            // Max number of children is M
            // make room for the next leaf reference (+1)
            children = new long[order + 1];

            for (int i = 0; i < keys.length - 1; i++)
                keys[i] = f.readInt();

            for (int i = 0; i < children.length - 1; i++)
                children[i] = f.readLong();

        }

        private void writeNode(long addr) throws IOException {
            f.seek(addr);
            f.writeInt(this.count);

            for (int i = 0; i < keys.length - 1; i++)
                f.writeInt(keys[i]);

            for (int i = 0; i < children.length - 1; i++)
                f.writeLong(children[i]);

        }
    }

    public BTree(String filename, int bsize) throws IOException {
        // bsize is the block size. This value is used to calculate the order
        // of the B+Tree
        // all B+Tree nodes will use bsize bytes
        // makes a new B+tree

        File bFile = new File(filename);

        if (bFile.exists())
            bFile.delete();

        f = new RandomAccessFile(bFile, "rw");

        // Start at the beginning of the file
        f.seek(0);

        // Set the root and free addresses in memory and in the file
        this.root = 0;
        f.writeLong(root);

        this.free = 0;
        f.writeLong(free);

        // Set the blocksize in memory and in the file
        this.blockSize = bsize;
        f.writeInt(blockSize);

        // Calculate the order
        this.order = bsize / 12;
    }

    public BTree(String filename) {
        // open an existing B+Tree
    }

    public void testPrint() throws IOException {
        BTreeNode testNode = new BTreeNode(20);

        System.out.println(testNode.count);
        for (int i = 0; i < testNode.keys.length - 1; i++) {
            System.out.println(testNode.keys[i]);
        }

        for (int i = 0; i < testNode.children.length; i++) {
            System.out.println(testNode.children[i]);
        }
    }

    public Stack<BTreeNode> findPath(long addr, int key) throws IOException {
        return findPathRec(addr, new Stack<BTreeNode>(), key);
    }

    private Stack<BTreeNode> findPathRec(long addr, Stack<BTreeNode> path, int key) throws IOException {
        if (addr == 0)
            return path;

        BTreeNode currNode = new BTreeNode(addr);

        path.push(currNode);

        // Leaf found
        if (currNode.count < 0)
            return path;

        for (int i = 0; i < Math.abs(currNode.count); i++) {
            if (key < currNode.keys[i]) {
                findPathRec(currNode.children[i], path, key);
                break;
                // End has been reached, so return last child
            }
            if (i == Math.abs(currNode.count) - 1) {
                findPathRec(currNode.children[i + 1], path, key);
                break;
            }
        }

        return path;
    }

    private void insertKeyAddressLeaf(BTreeNode currNode, int key, long addr) {
        for (int i = Math.abs(currNode.count); i > 0; i--) {
            if (currNode.keys[i - 1] > key) {
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

        currNode.count--;
    }

    private void insertKeyAddressNonLeaf(BTreeNode node, int key, long addr) {
        for (int i = (Math.abs(node.count)); i > 0; i--) {
            if (node.keys[i - 1] > key) {
                node.keys[i] = node.keys[i - 1];
                node.children[i + 1] = node.children[i];

                node.keys[i - 1] = key;
                node.children[i] = addr;
            } else {
                node.keys[i] = key;
                node.children[i + 1] = addr;
                break;
            }
        }

        node.count++;

    }

    public boolean hasRoom(BTreeNode node) {
        return Math.abs(node.count) < (order - 1);
    }

    public boolean insert(int key, long addr) throws IOException {
        /*
         * If key is not a duplicate add key to the B+tree
         * addr (in DBTable) is the address of the row that contains the key
         * return true if the key is added
         * return false if the key is a duplicate
         */

        long loc = 0;
        int val = 0;
        boolean split = false;

        // Root is empty
        if (root == 0) {
            BTreeNode newNode = new BTreeNode();
            long newNodeAddr = malloc();
            newNode.keys[0] = key;
            newNode.children[0] = addr;
            newNode.count = -1;
            root = newNodeAddr;
            newNode.writeNode(newNodeAddr);
            return true;
        }

        Stack<BTreeNode> path = findPath(root, key);

        // Should be a leaf node
        BTreeNode currNode = path.pop();

        // INSERTING INTO A LEAF
        // there is room in the node for a value
        if (hasRoom(currNode)) {
            insertKeyAddressLeaf(currNode, key, addr);
            currNode.writeNode(currNode.address);
            split = false;
        } else {
            // New Leaf Node for the split
            BTreeNode newNode = new BTreeNode();

            // Save Address prior to insertion. (The insertion causes an overflow that
            // overwrites existing values)
            newNode.children[newNode.children.length - 2] = currNode.children[currNode.children.length - 2];

            // Insert it in to the array
            insertKeyAddressLeaf(currNode, key, addr);

            // Calculate the new sizes of both nodes
            int newNodeCount = (int) Math.floor((double) currNode.count / 2);
            currNode.count = (currNode.count - newNodeCount);

            // Index in the new node
            int j = 0;

            // Copy the values to the new node (split the nodes)
            for (int i = (Math.abs(currNode.count)); i < currNode.keys.length; i++) {
                newNode.keys[j] = currNode.keys[i];
                newNode.children[j] = currNode.children[i];
                j++;
            }

            // Set the new count of the new node using formula above
            newNode.count = newNodeCount;

            // Val is the smallest value in the new node
            val = newNode.keys[0];

            loc = malloc();

            // Set the next page reference
            currNode.children[currNode.children.length - 2] = loc;

            // Write the node to the file
            currNode.writeNode(currNode.address);

            // Write the new node to file
            newNode.writeNode(loc);

            split = true;
        }

        // CLEANING UP PARENT NODES
        // Go through the rest of the search path and handle any BTree property issues
        while (!path.empty() && split) {
            currNode = path.pop();
            if (hasRoom(currNode)) {
                insertKeyAddressNonLeaf(currNode, val, loc);
                currNode.writeNode(currNode.address);
                split = false;
            } else {
                BTreeNode newNode = new BTreeNode();

                insertKeyAddressNonLeaf(currNode, val, loc);

                // New value is the middle value of the values in the node
                int newVal = currNode.keys[currNode.keys.length / 2];

                // Split the node
                currNode.count--;

                int j = 0;
                for (int i = (currNode.keys.length / 2) + 1; i < currNode.keys.length; i++) {
                    newNode.keys[j] = currNode.keys[i];

                    newNode.children[j] = currNode.children[i];

                    newNode.count++; // non-leaf node, so increase count (+)
                    currNode.count--; // non-leaf node, so decrease count (-)

                    j++;

                    if (i == currNode.keys.length - 1) {
                        newNode.children[j] = currNode.children[i + 1];
                    }
                }

                currNode.writeNode(currNode.address);

                loc = malloc();

                newNode.writeNode(loc);

                val = newVal;

                split = true;

            }
        }

        // The root was split, so create a new root and
        // "attach" both of the new nodes to this node
        if (split) {
            BTreeNode newRoot = new BTreeNode();

            // Insert the split node
            newRoot.keys[0] = val;
            newRoot.children[1] = loc;
            newRoot.children[0] = root;

            newRoot.count++;

            long newRootAddr = malloc();
            newRoot.writeNode(newRootAddr);
            root = newRootAddr;
            System.out.println();

        }

        return true;
    }

    public long malloc() throws IOException {
        long address = 0;

        BTreeNode freeNode;

        if (free == 0) {
            address = f.length();
        } else {
            freeNode = new BTreeNode(free);
            address = free;
            free = freeNode.children[freeNode.children.length - 2];
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

    public long search(int k) throws IOException {
        /*
         * This is an equality search
         * If the key is found return the address of the row with the key
         * otherwise return 0
         */
        return search(root, k);
    }

    private long search(long addr, int key) throws IOException {
        if (addr == 0)
            return 0;

        Stack<BTreeNode> path = findPath(addr, key);

        // If the tree contains the key, it would be in this node
        BTreeNode currNode = path.pop();

        for (int i = 0; i < Math.abs(currNode.count); i++) {
            if (currNode.keys[i] == key)
                return currNode.children[i];
        }

        // Key was not found
        return 0;
    }

    public LinkedList<Long> rangeSearch(int low, int high) throws IOException {
        // PRE: low <= high
        /*
         * return a list of row addresses for all keys in the range low to high
         * inclusive
         * return an empty list when no keys are in the range
         */

        LinkedList<Long> addresses = new LinkedList<Long>();

        Stack<BTreeNode> path = findPath(root, low);

        // If the tree contains the key, it would be in this node
        BTreeNode currNode = path.pop();

        int i = 0;

        while (currNode.keys[i] <= high) {
            if (currNode.keys[i] >= low) {
                addresses.add(currNode.children[i]);
            }

            i++;

            if (i == Math.abs(currNode.count)) {
                // End of list has been reached
                if (currNode.children[i] == 0)
                    break;
                currNode = new BTreeNode(currNode.children[currNode.children.length - 2]);
                i = 0;
            }
        }

        return addresses;
    }

    public void print() throws IOException {
        // print the B+Tree to standard output
        // print one node per line
        // This method can be helpful for debugging
        LinkedList<Long> q = new LinkedList<Long>();
        String spacing = "  ";

        // The root only has one node
        int numNodes = 1;

        q.add(root);

        while (!q.isEmpty()) {
            BTreeNode currNode = new BTreeNode(q.pop());

            System.out.print("| ");
            for (int i = 0; i < Math.abs(currNode.count); i++) {
                System.out.print(currNode.keys[i] + " ");

                if (currNode.count > 0)
                    q.add(currNode.children[i]);

            }

            numNodes--;

            // Spacing between nodes
            System.out.print("| ");

            if (currNode.count > 0) {
                q.add(currNode.children[currNode.count]);
            }

            if (numNodes == 0 && currNode.count > 0)
                System.out.println();

            if (numNodes <= 0 && currNode.count > 0) {
                spacing += "  ";
                numNodes = currNode.count + 1;
                System.out.print(spacing);
            }
        }

        System.out.println();

    }

    public void close() {
        // close the B+tree. The tree should not be accessed after close is called
    }
}
