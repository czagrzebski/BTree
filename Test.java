import java.io.*;

public class Test {
    public static void main(String[] args) throws IOException {
        BTree tree = new BTree("test.bin", 60);

       /*  int nums[] = {9, 5, 1, 13, 17, 2, 6, 7, 8, 3, 4, 10, 18, 11, 12, 14, 19, 15, 16, 20};

        for(int i=1; i <= 20; i++){
            tree.insert(i, i * 1000);
        }

        tree.remove(1);
        tree.remove(17);
        tree.remove(20);
        tree.remove(13);
        tree.remove(14);
        tree.insert(13, 13000);
        tree.remove(11);
        tree.remove(15);
        tree.remove(16);

        tree.print();
        tree.remove(12);

        tree.print();

        tree.insert(20, 20000);
        tree.insert(21, 30000);
        tree.insert(22, 40000);

        tree.print(); */

        tree.insert(15, 15000);

        tree.print();

        tree.remove(15);

        tree.insert(17, 15000);

     


       

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
