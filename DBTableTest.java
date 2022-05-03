import java.io.*;

public class DBTableTest {
    public static void main(String[] args) throws IOException {
        int[] fl = {10, 10};
        DBTable db = new DBTable("mysql", fl, 60);   
        
        char[][] person1 = {{
            'C', 'R', 'E', 'E', 'D', 0, 0, 0, 0, 0,
            'Z', 'A', 'G', 'R', 'Z', 'E', 'B', 'S', 'K', 'I'
        }};
        
        db.insert(17, person1);
        db.insert(13, person1);
        db.insert(11, person1);

        db.insert(34, person1);


        db.print();
    }
}
