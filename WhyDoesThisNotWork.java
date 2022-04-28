public class WhyDoesThisNotWork {
    public static void main(String[] args) {
        int[] sortedNumbers = {2, 3, 4, 5, 6};
        int key = 5;    

        printArray(sortedNumbers);

        for(int i=0; i < sortedNumbers.length; i++){
            if(sortedNumbers[i] == key){
                int j = i + 1;
                while(j < sortedNumbers.length){
                    sortedNumbers[j - 1] = sortedNumbers[j];
                    j++;
                }
                break;
            } else if(i == sortedNumbers.length - 1) {
                System.out.println("Not found");
            }
        }

        printArray(sortedNumbers);
    }

    public static void printArray(int[] array){
        for(int i=0; i < array.length; i++){
            System.out.print(array[i] + ", ");
        }
        System.out.println();
    }
}
