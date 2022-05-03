import java.util.*;
import java.io.*;

public class Testing {
    public static void main(String[] args) throws IOException {
        BTree b = new BTree("mytree", 60);

        for(int i=1; i <= 21; i++){
            b.insert(i, i * 1000);
        }

        b.print();
    }
}
