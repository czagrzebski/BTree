import java.io.*;

public class AdditionalTesting {
  public static void main(String[] args) throws IOException {
      BTree b = new BTree("testing.tree", 72);
      for(int i=1; i <= 25; i++){
        b.insert(i, i * 1000);
      }

     for(int i=25; i >= 16; i--){
         b.remove(i);
     }

  
        b.print();

    
      b.printCount(20);
  }  
}
