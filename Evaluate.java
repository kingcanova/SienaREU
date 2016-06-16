import java.io.*;
import javax.swing.*;
import java.util.*;
import java.nio.*;
import java.nio.file.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
/**
 * This program uses two JSON files, one with all of the results and one with a run that was complete,
 * and compares them writing 
 * 
 * @author Tristan Canova 
 * @version 6/3/16
 */
public class Evaluate
{
    /**
     * Main method that compares the two files
     */
    public static void main(String args[]) throws IOException
    {
        HashSet<String> results = new HashSet<String>();
        Scanner in = new Scanner(System.in);
        System.out.println("Please enter the name of the Run file to evaluate");
        String runFile = in.nextLine();
        BufferedReader br = null;//Objects to read
        BufferedReader br2 = null;
        PrintWriter output = null;//Object to write
        String line = "";//The results line to check the run line against
        String line2 = "";//The run line to be checked
        int user_ID = -1;
        Double P5 = -1.0;
        Double averageP5 = 0.0;

        BufferedReader runIn = new BufferedReader(new FileReader(
                    Paths.get("..\\DataFiles\\Runs\\" + runFile).toFile()));

        TreeMap<Integer, ArrayList<String>> runMap = new TreeMap<Integer, ArrayList<String>>();
        line = "";
        // Fill runMap with each profiles runs
        
        try
        {
            br = new BufferedReader(new FileReader(Paths.get("../DataFiles/realEval2015.txt").toFile()));
            //br2 =  new BufferedReader(new FileReader(Paths.get(runFile).toFile()));
            //buffered writer for output file
            //output = new PrintWriter(new FileWriter("evalOutput.txt"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        
        HashMap<Integer, HashSet<String>> relevantAttrs = new HashMap<>();
        while((line = br.readLine()) != null){
            String[] row = line.split("\t");
            int id = Integer.parseInt(row[0]);
            String attr = row[2];
            boolean relevant = Integer.parseInt(row[3]) == 1;
            if(relevant){
                if(relevantAttrs.get(id) == null){
                    relevantAttrs.put(id, new HashSet<String>());
                }
                HashSet<String> attrs = relevantAttrs.get(id);
                attrs.add(attr);
                relevantAttrs.put(id, attrs);
            }
        }
        
        br.close();
        
        line = "";
        while((line = runIn.readLine()) != null){
            String[] row = line.split("\t");
            int id = Integer.parseInt(row[0]);
            ArrayList<String> rankedAttractions = runMap.get(id);
            if(rankedAttractions == null){
                rankedAttractions = new ArrayList<String>();
            }
            String attr = row[2];
            rankedAttractions.add(attr);
            runMap.put(id, rankedAttractions);
        }

        runIn.close();
        double avgP5 = 0.0;
        int count = 0;
        Set<Integer> profileIDs = runMap.navigableKeySet();
        for(Integer id : profileIDs){
            count++;
            ArrayList<String> rankedAttractions = runMap.get(id);
            HashSet<String> relevant = relevantAttrs.get(id);
            double p5 = 1.0;
            for(int i =  0; i < 5; i++){
                String trec_id = rankedAttractions.get(i);
                if(relevant == null || !relevant.contains(trec_id)){
                    p5-=0.2;
                }
            }
            avgP5 += p5;
        }
        
        System.out.println(avgP5/count);
        br.close();
        //output.close();
    }

    public static void jsonResults() throws IOException
    {
        HashSet<String> results = new HashSet<String>();
        Scanner in = new Scanner(System.in);
        System.out.println("Please enter the name of the Run file to evaluate");
        String runFile = in.nextLine();
        BufferedReader br = null;//Objects to read
        BufferedReader br2 = null;
        PrintWriter output = null;//Object to write
        String line = "";//The results line to check the run line against
        String line2 = "";//The run line to be checked
        int user_ID = -1;
        Double P5 = -1.0;
        Double averageP5 = 0.0;

        BufferedReader runIn = new BufferedReader(new FileReader(
                    Paths.get(runFile).toFile()));

        HashMap<Integer, ArrayList<String>> runMap = new HashMap<Integer, ArrayList<String>>();
        line = "";
        // Fill runMap with each profiles runs
        JSONObject runResponse = null;
        JSONParser parser2 = new JSONParser();
        try
        {
            br = new BufferedReader(new FileReader(Paths.get("../DataFiles/realEval2015.txt").toFile()));
            //buffered writer for output file
            output = new PrintWriter(new FileWriter("evalOutput.txt"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        
        HashMap<Integer, HashSet<String>> relevantAttrs = new HashMap<>();
        while((line = br.readLine()) != null){
            String[] row = line.split("\t");
            int id = Integer.parseInt(row[0]);
            String attr = row[2];
            boolean relevant = Integer.parseInt(row[3]) == 1;
            if(relevant){
                if(relevantAttrs.get(id) == null){
                    relevantAttrs.put(id, new HashSet<String>());
                }
                HashSet<String> attrs = relevantAttrs.get(id);
                attrs.add(attr);
                relevantAttrs.put(id, attrs);
            }
        }
        
        int count = 0;
        double avgP5 = 0;
        while((line = runIn.readLine()) != null){
            count++;
            try 
            {
                runResponse = (JSONObject) parser2.parse(line);
            } 
            catch (ParseException pe) 
            {
                System.err.println("Error: could not parse JSON response:");
                System.out.println(line);
                System.exit(1);
            }
            
            int id = ((Long)runResponse.get("id")).intValue();
            HashSet<String> relevant = relevantAttrs.get(id);
            
            JSONObject o = (JSONObject)runResponse.get("body");
            
            JSONArray candidates = (JSONArray) o.get("suggestions");
            double p5 = 1.0;
            for(int i =  0; i < 5; i++){
                String trec_id = candidates.get(i).toString();
                if(relevant == null || !relevant.contains(trec_id)){
                    p5-=0.2;
                }
            }
            output.println(id + ": " + p5);
            avgP5 += p5;
        }
        System.out.println(avgP5 / count);

        runIn.close();
        br.close();
        output.close();
    }
}