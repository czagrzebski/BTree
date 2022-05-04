
/**
 * BTree.java 
 * A B+ Tree stored in a Random Access file
 * 
 * @author Creed Zagrzebski (zagrzebski1516@uwlax.edu)
 */

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

            // Read in the keys and children
            for (int i = 0; i < keys.length - 1; i++)
                keys[i] = f.readInt();

            for (int i = 0; i < children.length - 1; i++)
                children[i] = f.readLong();

        }

        // Write the node out to the file
        private void writeNode(long addr) throws IOException {
            f.seek(addr);
            f.writeInt(this.count);

            // Write the keys and children
            for (int i = 0; i < keys.length - 1; i++)
                f.writeInt(keys[i]);

            for (int i = 0; i < children.length - 1; i++)
                f.writeLong(children[i]);
        }

        /**
         * Returns true if the node has the minimum
         * number of keys required to satisfy BTree Properties
         * 
         * @return
         */
        private boolean minKeys() {
            return Math.abs(this.count) >= Math.ceil((double) order / 2) - 1
                    || this.address == root && Math.abs(this.count) >= 1;
        }

        /**
         * Returns true if the node contains the key
         * @param key
         * @return
         */
        private boolean contains(int key) {
            for (int i = 0; i < Math.abs(this.count); i++)
                if (this.keys[i] == key)
                    return true;

            return false;
        }

        /**
         * Returns true if the node has enough room to insert a key
         */
        private boolean hasRoom() {
            return Math.abs(this.count) < (order - 1);
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

        // Read in the root address, free address, and blockSize
        this.root = f.readLong();
        this.free = f.readLong();
        this.blockSize = f.readInt();

        // Calculate the order from the block size
        this.order = blockSize / 12;
    }

    /**
     * Returns a Stack containing the nodes between the root and
     * the leaf containing the key
     * 
     * @param addr Starting Address (typically the root)
     * @param key  Key to look for
     * @return Stack containing the releveant nodes
     * @throws IOException
     */
    public Stack<BTreeNode> findPath(long addr, int key) throws IOException {
        return findPathRec(addr, new Stack<BTreeNode>(), key);
    }

    // Private recursive method for finding the path
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

    /**
     * Inserts a key into a leaf
     * 
     * @param currNode Node to insert into
     * @param key      Key/Value
     * @param addr     Address in the DB Table
     */
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

    /**
     * Inserts a key into a non-leaf
     * 
     * @param node
     * @param key
     * @param addr
     */
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

   

    /**
     * Returns the left most value
     * 
     * @param node
     * @return leftmost value
     * @throws IOException
     */
    public int getLeftMostValue(BTreeNode node) throws IOException {
        if (node.count < 0) {
            return node.keys[0];
        } else {
            return getLeftMostValue(new BTreeNode(node.children[0]));
        }
    }

    /**
     * Inserts a key and address into the BTree.
     * Does not allow duplicates
     * 
     * @param key
     * @param addr
     * @return true - insert was successful
     *         false - the key is already in the tree
     * @throws IOException
     */
    public boolean insert(int key, long addr) throws IOException {
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

        if (currNode.contains(key))
            return false;

        // INSERTING INTO A LEAF
        // there is room in the node for a value
        if (currNode.hasRoom()) {
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
            if (currNode.hasRoom()) {
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

            // Allocate space for the new node
            long newRootAddr = malloc();
            newRoot.writeNode(newRootAddr);
            root = newRootAddr;
        }

        return true;
    }

    /**
     * Allocates space for the new node
     * 
     * @return Address of the new node
     * @throws IOException
     */
    public long malloc() throws IOException {
        long address;

        BTreeNode freeNode;

        if (free == 0) {
            address = f.length();
        } else {
            freeNode = new BTreeNode(free);
            address = free;
            free = freeNode.children[0];
        }

        return address;

    }

    /**
     * Returns the index in node where the address is located
     * 
     * @param node
     * @param address
     * @return
     */
    public int getIndexOfAddress(BTreeNode node, long address) {
        // Find the index where the child is stored (always a non-leaf)
        for (int i = 0; i < node.count + 1; i++) {
            if (node.children[i] == address) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Removes a key from the BTree if its exists and
     * returns the address of the row
     * 
     * @param key
     * @return Address of the row if found, else returns 0
     * @throws IOException
     */
    public long remove(int key) throws IOException {
        boolean tooSmall = false;
        long returnAddr = 0;

        Stack<BTreeNode> path = findPath(root, key);

        BTreeNode currNode = path.pop();

        // The last element in the tree is being removed
        if (currNode.count == -1 && currNode.keys[0] == key && currNode.address == this.root) {
            returnAddr = currNode.children[0];
            free(currNode.address);
            this.root = 0;
            return returnAddr;
        }

        // Find and remove the key from the leaf node
        for (int i = 0; i < Math.abs(currNode.count); i++) {
            // Key was found, so remove it
            if (currNode.keys[i] == key) {
                returnAddr = currNode.children[i];
                int j = i + 1;
                // Shift the values over (remove from leaf)
                while (j < Math.abs(currNode.count)) {
                    currNode.keys[j - 1] = currNode.keys[j];
                    currNode.children[j - 1] = currNode.children[j];
                    j++;
                }
                currNode.count++;
                currNode.writeNode(currNode.address);

                // The leaf node is now too small
                if (!currNode.minKeys()) {
                    tooSmall = true;
                }
                break;
            } else if (i == Math.abs(currNode.count) - 1) {
                return returnAddr; // never found key (returns 0)
            }

        }

        // Update the key of the parent of the node that had a value remove
        // If the leftmost value in a leaf was removed, then the parent key
        // needs to be change.
        if (!path.isEmpty()) {
            BTreeNode parentNode = path.peek();
            int childIndex = getIndexOfAddress(path.peek(), currNode.address);
            if (childIndex - 1 >= 0) {
                parentNode.keys[childIndex - 1] = currNode.keys[0];
                parentNode.writeNode(parentNode.address);
            }
        }

        while (!path.isEmpty() && tooSmall) {
            BTreeNode child = currNode; // save the currNode
            currNode = path.pop(); // get the parent node so we can do operations here

            // Find the index where the child is stored (always a non-leaf)
            int childIndex = getIndexOfAddress(currNode, child.address);
          
            // BORROWING
            // CHECK LEFT (< 0 then LEAF, ELSE NONLEAF)

            // A left node exists
            if (childIndex - 1 >= 0) {
                BTreeNode neighbor = new BTreeNode(currNode.children[childIndex - 1]);

                // Since a left node exists, see if it can be borrowed from.
                if (Math.abs(neighbor.count) - 1 >= Math.ceil((double) order / 2) - 1) {
                    if (child.count < 0) {
                        // Move value from left neighbor of child to child
                        // Rightmost value of neighbor
                        // LEAF
                        borrow(neighbor, child, Math.abs(neighbor.count) - 1);

                        currNode.keys[childIndex - 1] = child.keys[0];

                    } else {
                        // Move value from left neighbor of child to child
                        // For NON LEAF
                        // Rotation
                        int newParentKey = neighbor.keys[neighbor.count - 1];

                        borrow(neighbor, child, Math.abs(neighbor.count) - 1);

                        // "Rotate the values"
                        child.keys[0] = currNode.keys[childIndex - 1];
                        currNode.keys[childIndex - 1] = newParentKey;

                    }

                    // Save the nodes involved in borrowing
                    currNode.writeNode(currNode.address);
                    neighbor.writeNode(neighbor.address);
                    child.writeNode(child.address);
                    tooSmall = false;

                }
            }

            if (childIndex + 1 <= currNode.count && tooSmall) {
                BTreeNode neighbor = new BTreeNode(currNode.children[childIndex + 1]);

                // Since a right node exists, see if it can be borrowed from.
                if (Math.abs(neighbor.count) - 1 >= Math.ceil((double) order / 2) - 1) {
                    if (child.count < 0) {
                        // Move value from right neighbor of child to child
                        // leftmost value of neighbor
                        // LEAF
                        borrow(neighbor, child, 0);

                        currNode.keys[childIndex] = neighbor.keys[0];

                    } else {
                        // Move value/node from right neighbor of child to child
                        // leftmost value of neighbor
                        // NON-LEAF
                        int newParentKey = neighbor.keys[0];
                        borrow(neighbor, child, 0);

                        // "Rotate the values"
                        child.keys[child.count - 1] = currNode.keys[childIndex];
                        currNode.keys[childIndex] = newParentKey;

                    }

                    // Save the nodes involved in borrowing
                    currNode.writeNode(currNode.address);
                    neighbor.writeNode(neighbor.address);
                    child.writeNode(child.address);
                    tooSmall = false;
                }
            }

            if (tooSmall) {
                BTreeNode neighbor;
                // COMBINE HERE
                // Check if a left neighbor exists, or if a right neighbor exists.
                // Iterate through one of the neighbor nodes and add it into the child node.
                // Delete the key
                // from the parent node
                // Set the size of the node to 0

                // Check if the number of keys in the node is >= the minimum of keys required
                // If true, then the node is not too small
                // If false, then the node is too small

                if (childIndex - 1 >= 0) {
                    // Combine child into its left neighbor
                    neighbor = new BTreeNode(currNode.children[childIndex - 1]);
                    combine(neighbor, child);

                    // Adjust the link between the leaf nodes
                    if (neighbor.count < 0)
                        neighbor.children[neighbor.children.length - 2] = child.children[child.children.length - 2];

                    neighbor.writeNode(neighbor.address);

                    // Free the empty node
                    free(child.address);

                    // Remove the key shared between the two nodes
                    removeFromNonLeaf(currNode, currNode.keys[childIndex - 1]);

                } else {
                    // Combine right neighbor into child
                    neighbor = new BTreeNode(currNode.children[childIndex + 1]);
                    combine(child, neighbor);

                    // Adjust the link between the leaf nodes
                    if (child.count < 0)
                        child.children[child.children.length - 2] = neighbor.children[neighbor.children.length - 2];

                    child.writeNode(child.address);

                    // Free the empty node
                    free(neighbor.address);

                    // Remove the key shared between the two nodes
                    removeFromNonLeaf(currNode, currNode.keys[childIndex]);

                }

                currNode.writeNode(currNode.address);

                if (currNode.minKeys()) {
                    tooSmall = false;
                }

            }

        }

        // Then the root is too empty (root needs between 1 and M-1 keys)
        if (tooSmall) {
            long oldRoot = this.root;
            this.root = new BTreeNode(this.root).children[0];

            // free the old root
            free(oldRoot);

        }

        return returnAddr;
    }

    /**
     * Borrows a single key/address from a node
     * @param from Node to insert into (the node that is too small)
     * @param to Node to borrow from
     * @param borrowIndex The index to take the key/address from 
     */
    private void borrow(BTreeNode from, BTreeNode to, int borrowIndex) {
        // Borrowing from leafs, else nonleafs.
        if (to.count < 0) {
            int borrowKey = from.keys[borrowIndex];
            long borrowAddr = from.children[borrowIndex];

            int i = borrowIndex + 1;

            // Shift the values over 
            while (i < Math.abs(from.count)) {
                from.keys[i - 1] = from.keys[i];
                from.children[i - 1] = from.children[i];
                i++;
            }

            from.count++;

            // Inserted the borrowed value into the leaf node
            insertKeyLeaf(to, borrowKey, borrowAddr);

        } else if (borrowIndex == 0) {
            // A node is borrowing the left most key/address from a node to its right
            long borrowAddr = from.children[0];

            // Make room for the new address
            for (int i = 0; i < from.count - 1; i++) {
                from.keys[i] = from.keys[i + 1];
            }

            // Make room for the new address
            for (int i = 0; i < from.count; i++) {
                from.children[i] = from.children[i + 1];
            }

            to.children[to.count + 1] = borrowAddr;

            from.count--;
            to.count++;

        } else {
            // A node is borrowing the rightmost key/child from a node to its left
            long borrowAddr = from.children[borrowIndex + 1];

            // Shift values right in "to" to make room
            for (int i = to.count; i > 0; i--) {
                to.keys[i] = to.keys[i - 1];
            }

            for (int i = to.count + 1; i > 0; i--) {
                to.children[i] = to.children[i - 1];
            }

            to.children[0] = borrowAddr;

            to.count++;

            from.count--;
        }
    }

    /**
     * Merges/combines two nodes
     * 
     * @param to   Values/address will be inserted into this node
     * @param from Values/addresses will be copied from this node
     * @throws IOException
     */
    public void combine(BTreeNode to, BTreeNode from) throws IOException {
        // Combine two leaf nodes
        if (from.count < 0) {
            for (int i = 0; i < Math.abs(from.count); i++) {
                insertKeyLeaf(to, from.keys[i], from.children[i]);
            }
            from.count = 0;
        } else {
            // Combine two non-leaf nodes
            // Get the left most address of the node and insert it into the
            // node
            BTreeNode fromLeftChild = new BTreeNode(from.children[0]);
            to.children[to.count + 1] = from.children[0];

            // Find the new left most value and insert it into the node
            to.keys[to.count] = getLeftMostValue(fromLeftChild);
            to.count++;

            // Go through the nodes and combine the rest of the values
            int j = to.count;
            for (int i = 0; i < Math.abs(from.count); i++) {
                to.keys[j] = from.keys[i];
                to.children[j + 1] = from.children[i + 1];
                to.count++;
                j++;
            }

            from.count = 0;
        }

        to.writeNode(to.address);
        from.writeNode(from.address);

    }

    /**
     * Removes a key from a non leaf
     * 
     * @param node
     * @param key
     * @throws IOException
     */
    public void removeFromNonLeaf(BTreeNode node, int key) throws IOException {
        // Search for the key. If it is found, remove it.a
        // Else, return 0 (not found)
        for (int i = 0; i < Math.abs(node.count) + 1; i++) {
            if (node.keys[i] == key) {
                int j = i + 1;
                while (j < Math.abs(node.count)) {
                    node.keys[j - 1] = node.keys[j];
                    node.children[j] = node.children[j + 1];
                    j++;
                }
                node.count--; // Leaf, so this removes 1 from the size
                node.writeNode(node.address);
            }
        }

    }

    /**
     * Prints the free list
     * 
     * @throws IOException
     */
    public void printFreeList() throws IOException {
        System.out.println();
        printFreeList(this.free);
        System.out.println();
    }

    private void printFreeList(long addr) throws IOException {
        if (addr == 0) {
            return;
        } else {
            BTreeNode node = new BTreeNode(addr);
            System.out.print(node.address + ", ");
            printFreeList(node.children[0]);

        }
    }

    /**
     * Frees a node from the BTree and adds
     * it to the free list
     * 
     * @param addr Address of the node to free
     * @throws IOException
     */
    private void free(long addr) throws IOException {
        BTreeNode toFree = new BTreeNode(addr);
        toFree.children[0] = free;
        free = addr;

        toFree.writeNode(addr);
    }

    /**
     * Finds the address of the key, if it exists.
     * 
     * @param k
     * @return Address of the key, if it exists
     *         Else, returns 0.
     * @throws IOException
     */
    public long search(int k) throws IOException {
        return search(root, k);
    }

    private long search(long addr, int key) throws IOException {
        if (addr == 0)
            return 0;

        Stack<BTreeNode> path = findPath(addr, key);

        // The path is empty
        if (path.size() == 0)
            return 0;

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

    /**
     * Returns a linkedlist containing addresses of values within the specified
     * range
     * (low to high inclusive). Returns an empty list if nothing is found
     * 
     * @param low
     * @param high
     * @return A LinkedList of all addresses within the range
     *         Empty list if nothing is found
     * @throws IOException
     */
    public LinkedList<Long> rangeSearch(int low, int high) throws IOException {
        // PRE: low <= high
        LinkedList<Long> addresses = new LinkedList<Long>();

        Stack<BTreeNode> path = findPath(root, low);

        if (path.size() == 0)
            return addresses;

        // If the tree contains the key, it would be in this node
        BTreeNode currNode = path.pop();

        int i = 0;

        while (currNode.keys[i] <= high) {
            // Key is within range
            if (currNode.keys[i] >= low)
                addresses.add(currNode.children[i]);

            i++;

            // End of list has been reached, so add the last child
            // to the LinkedList
            if (i == Math.abs(currNode.count)) {
                if (currNode.children[currNode.children.length - 2] == 0)
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

            System.out.print("[" + currNode.address + ": ");
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
        f.writeLong(this.free);

        f.close();
    }
}
