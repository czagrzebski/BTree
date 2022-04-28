import java.io.*;
import java.util.Stack;

public class Test {
    public static void main(String[] args) throws IOException {
        BTree tree = new BTree("test.bin", 60);
        //11, 9, 14, 4, 2, 15
    
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
        tree.insert(33, 3300);      

      

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

        tree.print();

        tree.remove(44);

        tree.remove(35);

        tree.print();

        tree.insert(44, 4400);
        tree.insert(35, 3700);

        tree.print();

        tree.remove(32);
        tree.remove(33);

        tree.print();



       

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
