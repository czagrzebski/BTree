import java.io.*;
import java.util.Stack;

public class Test {
    public static void main(String[] args) throws IOException {
        BTree tree = new BTree("test.bin", 72);

        
        tree.insert(2, 2002);

        tree.insert(10, 2003);

        tree.insert(13, 2005);

        tree.insert(9, 2006);
      
        tree.insert(15, 2008);

        tree.insert(1, 2009);

    

        tree.testPrint();

        

      /*   int[] sortedArray = new int[5];
        long[] childArray = new long[6];


        sortedArray[0] = 2;
        sortedArray[1] = 8;
        sortedArray[2] = 14;
        sortedArray[3] = 20;

        childArray[0] = 1000;
        childArray[1] = 2000;
        childArray[2] = 3000;
        childArray[3] = 4000;

        
        
        int size = 4;

        int value = 12;
        long addr = 6000;

        printArray(sortedArray);
        printChildArray(childArray);
 
        for(int i=size; i > 0; i--){
            if(sortedArray[i - 1] > value) {                
                sortedArray[i] = sortedArray[i - 1];
                sortedArray[i - 1] = value;

                childArray[i] = childArray[i - 1];
                childArray[i - 1] = addr;
            } else {
                sortedArray[i] = value;
                childArray[i] = addr;
                break;
            }
    
            
        } 

        System.out.println();

        printArray(sortedArray);
        printChildArray(childArray); */


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
