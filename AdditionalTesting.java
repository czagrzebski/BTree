import java.io.*;

public class AdditionalTesting {
  public static void main(String[] args) throws IOException {
      BTree b = new BTree("testing.tree", 60);
      
      for(int i=7; i <= 20; i++){
        b.insert(i, i * 1000);
      }

      b.insert(5, 5000);
      b.insert(4, 4000);
      b.insert(3, 3000);

      

      /* for(int i=15; i >= 1; i--){
         b.insert(i, i * 1000);
      }  */

      /* b.insert(16, 16000);
      b.insert(17, 17000);
      b.insert(18, 18000);
      b.insert(19, 19000);
      b.insert(20, 20000);

      b.remove(3);
      b.remove(2);
      b.remove(1);

      b.insert(1, 1000);
      b.insert(2, 2000);
   
      b.remove(14);

      b.remove(20);
      b.remove(19);
      b.remove(18);
      b.remove(17); */


     b.print();
      
     b.remove(13);
     
     b.print();

    
     // b.printCount(20);
  }  
}
