# B+ Tree (Project 4)

[Project Overview](https://cs.uwlax.edu/~tgendreau/cs340/p4.pdf) 

## DB 
- Create a new file
    - [ ] Open the file


## BTree
- Remove
  - [x] Remove the key from the node (if it exists)
  - [ ] Borrowing
    - [ ] Check if borrowing is possible from either neighbor
      - [ ] Check if it is possible to access the left sibling
        - [ ] If so, check if a value can be removed from it
        - [ ] Remove the value and shift 
      - [ ] Check if it is possible to access the right sibling
        - [ ] If so, check if a value can be removed from it
        - [ ] Remove the value and shift


- Notes
  - Combine
    - Check if you combine right first (Pull child's right into itself)
    - Then check if you can combine right (Pull child into child's left neighbor)
    - Change References


- Testing BTree Removal
  - Borrow Right (from leaf)
  - Borrow Left (from leaf)
  - Combine Right (from leaf)
  - Combine Left (from left)
  
  - Borrow Right (from non-leaf)
  - Borrow Left (from non-leaf)
  - Combine Right (from non-leaf)
  - Combine Left (from non-leaf)


- Test Free List for BTree