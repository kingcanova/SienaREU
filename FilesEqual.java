import java.util.*;
import java.io.*;
import java.nio.file.*;
/**
 * Simple program for determining if two files have the same content (for convenience purposes)
 */
public class FilesEqual
{
    public static void main(String[] args) throws FileNotFoundException{
        Scanner in = new Scanner(System.in);

        System.out.print("Enter the name of the first file: ");
        String file = in.next();

        System.out.print("Enter the name of the second file: ");
        String file2 = in.next();

        try{
            byte[] arr1 = Files.readAllBytes(Paths.get(file));
            byte[] arr2 = Files.readAllBytes(Paths.get(file2));
            System.out.println(Arrays.equals(arr1,arr2));
        }catch(IOException e){
            e.printStackTrace();
        }
        System.out.println("DONE");
        in.close();
    }

    public static void main2() throws IOException{
        Scanner in = new Scanner(System.in);

        System.out.print("Enter the name of the first file: ");
        String file = in.next();
        BufferedReader in2 = new BufferedReader(new FileReader(Paths.get(file).toFile()));

        System.out.print("Enter the name of the second file: ");
        String file2 = in.next();
        BufferedReader in3 = new BufferedReader(new FileReader(Paths.get(file2).toFile()));

        boolean same = true;
        String line;
        while((line = in2.readLine()) != null){
            //String line = in2.nextLine();
            //System.out.println(line);
            String line2 = in3.readLine();
            if(line2 != null){
                //line2 = in3.nextLine();
                //System.out.println(line2);
                if(!line.equals(line2)){
                    same = false;
                    System.out.println(line);
                    System.out.println(line2);
                    System.out.println();
                }
            }
            else{
                same = false;
                break;
            }
        }
        System.out.println(same);
        System.out.println("DONE");
        in.close();
        in2.close();
        in3.close();
    }
}
