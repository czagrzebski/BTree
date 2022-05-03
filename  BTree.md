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
  - [x] Borrow Right (from leaf)
  - [x] Borrow Left (from leaf)
  - [x] Combine Right (from leaf)
  - [x] Combine Left (from left)
  
  - [x] Borrow Right (from non-leaf)
  - [x] Borrow Left (from non-leaf)
  - [x] Combine Right (from non-leaf)
  - [x] Combine Left (from non-leaf)

- Questions for Gendreau
  - How do I know how many keys to borrow?
  - Free List for DB Table Row
  - Test
  - Ask About Special Case with Borrow from Non Leaf


- Test Free List for BTree