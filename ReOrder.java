import java.util.*;
import java.nio.*;
import java.nio.file.*;
import java.io.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
/**
 * This program merely outputs a sorted output json results file from an unsorted json results.
 */
public class ReOrder
{
    public static void reOrder(String inputFile, String outputFile)throws IOException{
        BufferedReader in = new BufferedReader(new FileReader(
                    Paths.get(inputFile).toFile()));
        TreeMap<Integer, String> map = new TreeMap<Integer, String>();
        JSONParser parser = new JSONParser();
        String line = "";
        while((line = in.readLine()) != null){
            JSONObject response = null;
            try{
                response = (JSONObject) parser.parse(line);
            } catch (ParseException pe) {
                System.err.println("Error: could not parse JSON response:");
                System.out.println(line);
                System.exit(1);
            }
            int id = ((Long)response.get("id")).intValue();
            map.put(id, line);
        }
        
        PrintWriter pw = new PrintWriter(outputFile);
        Set<Integer> people = map.navigableKeySet();
        for(Integer person : people){
            pw.println(map.get(person));
        }
        in.close();
        pw.close();
    }
    
    public static void blah(String s){
        String[] row = s.split(" - ");
        System.out.println(row[0]);
    }
}
