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

        private boolean contains(int key) {
            for (int i = 0; i < Math.abs(this.count); i++)
                if (this.keys[i] == key)
                    return true;

            return false;
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

    /**
     * Ensures that the values in non-leaf nodes satisfy
     * B+ Tree Properties
     */
    public void checkValues(BTreeNode node) throws IOException {
        if (node.count > 0) {
            for (int i = 0; i < node.count; i++) {
                BTreeNode childOfChild = new BTreeNode(node.children[i + 1]);
                if (childOfChild.count > 0) {
                    node.keys[i] = getLeftMostValue(childOfChild);
                } else {
                    break;
                }
            }
            node.writeNode(node.address);
        }
    }

    public int getLeftMostValue(BTreeNode node) throws IOException {
        if (node.count < 0) {
            return node.keys[0];
        } else {
            return getLeftMostValue(new BTreeNode(node.children[0]));
        }
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

        if (currNode.contains(key))
            return false;

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
            free = freeNode.children[0];
        }

        return address;

    }

    public int getIndexOfAddress(BTreeNode node, long address) {
        // Find the index where the child is stored (always a non-leaf)
        int childIndex = -1;
        for (int i = 0; i < node.count + 1; i++) {
            if (node.children[i] == address) {
                return i;
            }
        }

        return -1;
    }

    public long remove(int key) throws IOException {
        /*
         * If the key is in the Btree, remove the key and return the address of the
         * row
         * return 0 if the key is not found in the B+tree
         */

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

        for (int i = 0; i < Math.abs(currNode.count); i++) {
            if (currNode.keys[i] == key) {
                int j = i + 1;
                while (j < Math.abs(currNode.count)) {
                    currNode.keys[j - 1] = currNode.keys[j];
                    currNode.children[j - 1] = currNode.children[j];
                    j++;
                }
                currNode.count++;
                currNode.writeNode(currNode.address);

                // Too Small
                if (!currNode.minKeys()) {
                    tooSmall = true;
                }
                break;
            } else if (i == Math.abs(currNode.count) - 1) {
                return returnAddr; // never found key
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
            int childIndex = -1;
            for (int i = 0; i < currNode.count + 1; i++) {
                if (currNode.children[i] == child.address) {
                    childIndex = i;
                    break;
                }
            }

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

                        child.keys[0] = currNode.keys[childIndex - 1];

                        currNode.keys[childIndex - 1] = newParentKey;

                    }

                    // Save the nodes
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
                        int newParentKey = neighbor.keys[0];
                        borrow(neighbor, child, 0);

                        child.keys[child.count - 1] = currNode.keys[childIndex];
                        currNode.keys[childIndex] = newParentKey;

                    }

                    // Save the nodes
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
                // TODO: Destroy the node and add it to the free list

                // Check if the number of keys in the node is >= the minimum of keys required
                // If true, then the node is not too small
                // If false, then the node is too small

                // Borrow from right
                if (childIndex - 1 >= 0) {

                    neighbor = new BTreeNode(currNode.children[childIndex - 1]);
                    combine(neighbor, child);

                    if (neighbor.count < 0)
                        neighbor.children[neighbor.children.length - 2] = child.children[child.children.length - 2];

                    neighbor.writeNode(neighbor.address);

                    free(child.address);

                    // Remove the key shared between the two nodes
                    removeFromNonLeaf(currNode, currNode.keys[childIndex - 1]);

                    // free here

                } else {
                    neighbor = new BTreeNode(currNode.children[childIndex + 1]);
                    combine(child, neighbor);

                    if (child.count < 0)
                        child.children[child.children.length - 2] = neighbor.children[neighbor.children.length - 2];

                    child.writeNode(child.address);

                    free(neighbor.address);

                    // Remove the key shared between the two nodes
                    removeFromNonLeaf(currNode, currNode.keys[childIndex]);

                    // free here
                }

                currNode.writeNode(currNode.address);

                if (currNode.minKeys()) {
                    tooSmall = false;
                }

            }

        }

        while(!path.isEmpty()){
            BTreeNode node = path.pop();
            //checkValues(node);
        }

        // Then the root is too empty (root needs between 1 and M-1 keys)
        if (tooSmall) {
            long oldRoot = this.root;
            this.root = new BTreeNode(this.root).children[0];

            free(oldRoot);
            // free here
        }

        return returnAddr;
    }

    private void borrow(BTreeNode from, BTreeNode to, int borrowIndex) {
        // Borrowing from leafs, else nonleafs.
        if (to.count < 0) {
            int borrowKey = from.keys[borrowIndex];
            long borrowAddr = from.children[borrowIndex];

            int i = borrowIndex + 1;

            while (i < Math.abs(from.count)) {
                from.keys[i - 1] = from.keys[i];
                from.children[i - 1] = from.children[i];
                i++;
            }

            from.count++;

            insertKeyLeaf(to, borrowKey, borrowAddr);

        } else if (borrowIndex == 0) {
            long borrowAddr = from.children[0];
            

            for (int i = 0; i < from.count - 1; i++) {
                from.keys[i] = from.keys[i + 1];
            }

            for (int i = 0; i < from.count; i++) {
                from.children[i] = from.children[i + 1];
            }

            to.children[to.count + 1] = borrowAddr;

            from.count--;
            to.count++;

        } else {
            // Borrow the rightmost key/child from a node
            long borrowAddr = from.children[borrowIndex + 1];

            // Shift the keys over
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

    // Swaps two nodes
    public void combine(BTreeNode to, BTreeNode from) throws IOException {
        if (from.count < 0) {
            for (int i = 0; i < Math.abs(from.count); i++) {
                insertKeyLeaf(to, from.keys[i], from.children[i]);
            }
            from.count = 0;
        } else {
            BTreeNode fromLeftChild = new BTreeNode(from.children[0]);
            to.children[to.count + 1] = from.children[0];
            to.keys[to.count] = getLeftMostValue(fromLeftChild);
            to.count++;
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

    public void printFreeList() throws IOException {
        System.out.println();
        printFreeListRec(this.free);
        System.out.println();
    }

    private void printFreeListRec(long addr) throws IOException {
        if (addr == 0) {
            return;
        } else {
            BTreeNode node = new BTreeNode(addr);
            System.out.print(node.address + ", ");
            printFreeListRec(node.children[0]);

        }
    }

    private void free(long addr) throws IOException {
        BTreeNode toFree = new BTreeNode(addr);
        toFree.children[0] = free;
        free = addr;

        toFree.writeNode(addr);
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

    public void printCount(long addr) throws IOException {
        System.out.println((new BTreeNode(addr)).count);
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
