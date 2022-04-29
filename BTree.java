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

    public BTree(String filename) throws IOException {
        // open an existing B+Tree
        File bFile = new File(filename);
        f = new RandomAccessFile(bFile, "rw");

        f.seek(0);

        this.root = f.readLong();
        this.free = f.readLong();
        this.blockSize = f.readInt();

        // Calculate the order
        this.order = blockSize / 12;
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

    private void insertKeyLeaf(BTreeNode currNode, int key, long addr) {
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

    private void insertKeyNonLeaf(BTreeNode node, int key, long addr) {
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
            insertKeyLeaf(currNode, key, addr);
            currNode.writeNode(currNode.address);
            split = false;
        } else {
            // New Leaf Node for the split
            BTreeNode newNode = new BTreeNode();

            // Save Address prior to insertion. (The insertion causes an overflow that
            // overwrites existing values)
            newNode.children[newNode.children.length - 2] = currNode.children[currNode.children.length - 2];

            // Insert it in to the array
            insertKeyLeaf(currNode, key, addr);

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
                insertKeyNonLeaf(currNode, val, loc);
                currNode.writeNode(currNode.address);
                split = false;
            } else {
                BTreeNode newNode = new BTreeNode();

                insertKeyNonLeaf(currNode, val, loc);

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

    public long remove(int key) throws IOException {
        /*
         * If the key is in the Btree, remove the key and return the address of the
         * row
         * return 0 if the key is not found in the B+tree
         */

        Stack<BTreeNode> path = findPath(root, key);
        boolean tooSmall = false;

        // Remove from a sorted array

        BTreeNode currNode = path.pop();

        // Search for the key. If it is found, remove it.
        // Else, return 0 (not found)
        for(int i=0; i < Math.abs(currNode.count); i++){
            if(currNode.keys[i] == key){
                int j = i + 1;
                while(j < Math.abs(currNode.count)){
                    currNode.keys[j - 1] = currNode.keys[j];
                    currNode.children[j - 1] = currNode.children[j];
                    j++;
                }
                currNode.count++; //Leaf, so this removes 1 from the size
                currNode.writeNode(currNode.address);

                // Too Small
                if(Math.abs(currNode.count) < Math.ceil( (double) order / 2) - 1){
                    tooSmall = true;
                }
                break;

            } else if(i == Math.abs(currNode.count) - 1) {
                return 0;
            }
        }

        while(!path.isEmpty() && tooSmall){
            BTreeNode child = currNode; //save the currNode
            currNode = path.pop(); // get the parent node so we can do operations here

            // Find the index where the child is stored (always a non-leaf)
            int childIndex = -1;
            for(int i=0; i < currNode.count + 1; i++){
                if(currNode.children[i] == child.address){
                    childIndex = i;
                    break;
                } 
            }

            // Check Neighbors of Child
            BTreeNode neighbor;

            // Check to see if I can remove from the left neighbor
            // Check if it is a valid index and that it has enough children
            // This is all non-leaf logic
            // Borrow differentiates between leaf and non-leaf
            if(childIndex - 1 >= 0){
                neighbor = new BTreeNode(currNode.children[childIndex - 1]);
                
                //Borrowing is possible
                if(Math.abs(neighbor.count) - 1 >= Math.ceil( (double) order / 2) - 1){
                    borrow(neighbor, child, Math.abs(neighbor.count) - 1);
                    tooSmall = false;

                    currNode.keys[childIndex - 1] = child.keys[0];

                    currNode.writeNode(currNode.address);
                }
            } 
   
            // Check Right neighbor
            // IMPLEMENT HERE
            if(childIndex < currNode.count && tooSmall) {
                neighbor = new BTreeNode(currNode.children[childIndex + 1]);

                 //Borrowing is possible
                 if(Math.abs(neighbor.count) - 1 >= Math.ceil( (double) order / 2) - 1){
                    borrow(neighbor, child, 0);
                    tooSmall = false;

                    currNode.keys[childIndex] = neighbor.keys[0];

                    currNode.writeNode(currNode.address);
                }

            }


            //IF IT IS STILL TOO SMALL, then I NEED TO COMBINE

            if(tooSmall){
                //COMBINE HERE
                // Check if a left neighbor exists, or if a right neighbor exists. 
                // Iterate through one of the neighbor nodes and add it into the child node. Delete the key
                // from the parent node
                // Set the size of the node to 0
                // Destroy the node and add it to the free list
                
                // Check if the number of keys in the node is >= the minimum of keys required
                // If true, then the node is not too small
                // If false, then the node is too small

                // Borrow from right
                if(childIndex < currNode.count){
                    neighbor = new BTreeNode(currNode.children[childIndex + 1]);
                    combine(child, neighbor);

                    child.children[child.children.length - 2] = neighbor.children[neighbor.children.length - 2];

                    child.writeNode(child.address);


                } else {
                    neighbor = new BTreeNode(currNode.children[childIndex - 1]);
                    combine(neighbor, child);

                    neighbor.children[neighbor.children.length - 2] = child.children[child.children.length - 2];

                    neighbor.writeNode(neighbor.address);
                    
                }
            }

    
        }


        return 0;
    }

    /**
     * Borrows a key/address from a neighbor node and puts it into the child
     * @param neighbor BTreeNode to take the key/address from. 
     * @param child BTreeNode to put the key in.
     * @param borrowIndex The index of the key in the neighbor
     * @throws IOException
     */
    public void borrow(BTreeNode neighbor, BTreeNode child, int borrowIndex) throws IOException{
        int key;
        long addr;

        // Leaf Node, else its a non-leaf
        if(child.count < 0){
            key = neighbor.keys[borrowIndex];
            addr = neighbor.children[borrowIndex];

            int i = borrowIndex + 1;

            while(i < Math.abs(neighbor.count)){
                neighbor.keys[i - 1] = neighbor.keys[i];
                neighbor.children[i - 1] = neighbor.children[i];
                i++;
            }

            neighbor.count++;

            insertKeyLeaf(child, key, addr);
            

        } else {
            key = neighbor.keys[borrowIndex];
            addr = neighbor.children[borrowIndex + 1];
            neighbor.count--;

            insertKeyNonLeaf(child, key, addr);
        }

        neighbor.writeNode(neighbor.address);
        child.writeNode(child.address);

    }

    // Swaps two nodes
    public void combine(BTreeNode to, BTreeNode from) throws IOException {
        if(from.count < 0){
            for(int i=0; i < Math.abs(from.count); i++){
                insertKeyLeaf(to, from.keys[i], from.children[i]);
            }

            from.count = 0;
        
        }

        to.writeNode(to.address);
        from.writeNode(from.address);

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

        // Search the keys in the node for a match
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
            //Key is within range
            if (currNode.keys[i] >= low) 
                addresses.add(currNode.children[i]);
            
            i++;

            // End of list has been reached, so add the last child 
            // to the LinkedList
            if (i == Math.abs(currNode.count)) {
                if (currNode.children[i] == 0)
                    break;

                // Go to the next leaf
                currNode = new BTreeNode(currNode.children[currNode.children.length - 2]);
                i = 0; // reset index
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
        int currNumNodes = 1;
        int level = 0;
        int nextNumberNodes = 0;

        System.out.println("\n==========BTree==========");

        q.add(root);

        System.out.print(level + ": ");
        while (!q.isEmpty()) {
            BTreeNode currNode = new BTreeNode(q.pop());

            System.out.print("[ ");
            for (int i = 0; i < Math.abs(currNode.count); i++) {
                System.out.print(currNode.keys[i] + " ");

                if (currNode.count > 0) {
                    q.add(currNode.children[i]);
                    nextNumberNodes++;
                }

            }

            currNumNodes--;

            // Spacing between nodes
            System.out.print("] ");

            if (currNode.count > 0) {
                q.add(currNode.children[currNode.count]);
                nextNumberNodes++;
            }

            if (currNode.count < 0 && currNode.children[currNode.children.length - 2] != 0) {
                System.out.print("-> ");
            }

            if (currNumNodes == 0 && currNode.count > 0)
                System.out.println();

            if (currNumNodes <= 0 && currNode.count > 0) {
                spacing += "  ";
                currNumNodes = nextNumberNodes;
                nextNumberNodes = 0;
                level++;
                System.out.print(level + ":" + spacing);
            }
        }

        System.out.println("\n=========================");

    }

    public void close() throws IOException {
        // close the B+tree. The tree should not be accessed after close is called
        f.seek(0);
        f.writeLong(this.root);
        f.writeLong( this.free);

        f.close();
    }
}
