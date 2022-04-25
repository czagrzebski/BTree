public class WhyDoesThisNotWork {
    public static void main(String[] args) {
        int[] keys = new int[4];

        int[] children = new int[6];

        int key = 9;
        int addr = 75;

        keys[0] = 5;
        keys[1] = 8;
        keys[2] = 12;
        
        children[0] = 10;
        children[1] = 20;
        children[2] = 30;
        children[3] = 40;


        for(int i=(3); i > 0; i--){
            if(keys[i - 1] > key){
                keys[i] = keys[i - 1];
                children[i + 1] = children[i];

                keys[i - 1] = key;
                children[i] = addr;
            } else {
                keys[i] = key;
                children[i + 1] = addr;
                break;
            }
        }

        printArray(keys);
        printArray(children);
    }

    public static void printArray(int[] array){
        for(int i=0; i < array.length; i++){
            System.out.print(array[i] + ", ");
        }
        System.out.println();
    }
}
