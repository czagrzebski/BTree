import java.io.*;
import java.util.Stack;

public class Test {
    public static void main(String[] args) throws IOException {
        BTree tree = new BTree("test.bin", 60);
        //11, 9, 14, 4, 2, 15
    /* 
        tree.insert(11, 2000);
        tree.insert(9, 2100);
        tree.insert(14, 2200);
        tree.insert(4, 2300);
        tree.insert(2, 2400); 
        tree.insert(15, 2600); 
        tree.insert(21, 2700); 
        tree.insert(25, 2800); 
        tree.insert(28, 2900); 
        tree.insert(32, 3000); 
        tree.insert(35, 3100); 
        tree.insert(39, 3200); 
        tree.insert(44, 3300);
        tree.insert(33, 3300);   */    

      

      /*   tree.insert(1, 1000);
        tree.insert(2, 2000);
        tree.insert(3, 3000);
        tree.insert(4, 4000);
        tree.insert(5, 5000);
        tree.insert(6, 6000);
        tree.insert(7, 7000);
        tree.insert(8, 8000);
        tree.insert(9, 9000); */



        //System.out.println(tree.rangeSearch(3, 6));

       /*  tree.print();

        tree.remove(44);

        tree.remove(35);

        tree.print();

        tree.insert(44, 4400);
        tree.insert(35, 3700);

        tree.print();

        tree.remove(32);
        tree.remove(33);

        tree.print(); */


        //7, 14, 21, 25, 29, 34, 3, 2, 1

      /*   tree.insert(7, 1000);
        tree.insert(14, 2000);
        tree.insert(21, 3000);
        tree.insert(25, 4000);
        tree.insert(29, 5000);
        tree.insert(34, 6000);
        tree.insert(3, 7000);
        tree.insert(2, 8000);
        tree.insert(1, 9000);



        //1, 

        tree.remove(21);

        tree.remove(14);

        tree.remove(25);

    
  tree.print();
        tree.remove(7);

       // tree.print();

        
       tree.print();

        tree.remove(29);

        tree.print();

      //  tree.print();

        tree.remove(3);

        tree.print(); */



        int nums[] = {9, 5, 1, 13, 17, 2, 6, 7, 8, 3, 4, 10, 18, 11, 12, 14, 19, 15, 16, 20};

        /* for(int i=0; i < nums.length; i++){
            tree.insert(nums[i], nums[i] * 1000);
        } */
/* 
        tree.insert(22, 22000);

        tree.remove(17);

        tree.print();

        tree.remove(16);


        tree.remove(18);

        tree.print();

        tree.remove(19); */

        for(int i=1; i <= 19; i++){
            tree.insert(i, i*1000);
        }

        tree.remove(19);

        tree.remove(17);

        tree.print();

        tree.remove(5);

       // tree.remove(9);

       

        tree.print();
        //Remove 3, 7, 25, 29, 2
       

    }


    public static void printArray(int[] array){
        for(int i=0; i < array.length; i++){
            System.out.print(array[i] + ", ");
        }
        System.out.println();
    }

    public static void printChildArray(long[] array){
        for(int i=0; i < array.length; i++){
            System.out.print(array[i] + ", ");
        }
        System.out.println();
    }
}
