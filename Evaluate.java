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
        int count = 0;//COUNTING FOR AVERAGE P@5
        Double averageP5 = 0.0;

        BufferedReader runIn = new BufferedReader(new FileReader(
                    Paths.get("Runs\\" + runFile).toFile()));

        HashMap<Integer, ArrayList<String>> runMap = new HashMap<Integer, ArrayList<String>>();
        line = "";
        // Fill runMap with each profiles runs
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
        line = "";
        try
        {
            br = new BufferedReader(new FileReader(Paths.get("../DataFiles/SurveyResults.json").toFile()));
            //br2 =  new BufferedReader(new FileReader(Paths.get(runFile).toFile()));
            //buffered writer for output file
            output = new PrintWriter(new FileWriter("evalOutput.txt"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        JSONParser parser = new JSONParser();
        JSONParser parser2 = new JSONParser();
        JSONObject response = null;
        //JSONObject runResponse = null;
        while((line = br.readLine())!=null)
        {
            count++;
            results.clear();
            //JSON parsing for response
            try 
            {
                response = (JSONObject) parser.parse(line);
                //line2 = br2.readLine();//The run line to check against results
                //runResponse = (JSONObject) parser2.parse(line2);
            } 
            catch (ParseException pe) 
            {
                System.err.println("Error: could not parse JSON response:");
                System.out.println(line);
                System.exit(1);
            }
            catch (NullPointerException e) 
            {
                System.err.println("Error: null pointer" + e);
            }
            //obtain the User ID
            user_ID = ((Long)response.get("id")).intValue();
            //obtain JSON array of result TREC ID's
            JSONArray resultIDs = (JSONArray) response.get("results");
            for(int i =  0; i < resultIDs.size(); i++)
            {
                String trec_id = resultIDs.get(i).toString();
                results.add(trec_id);
            }

            //JSONObject body = (JSONObject) runResponse.get("body");
            //JSONArray runIDs = (JSONArray) body.get("suggestions");
            ArrayList<String> runIDs = runMap.get(user_ID);
            P5 = 1.0;
            for(int i = 0; i < 5; i++)
            {
                String runID = runIDs.get(i);
                if(!results.contains(runID))
                {
                    P5 = P5 - .2;
                }
            }
            averageP5 += P5;
            output.println(user_ID + ": P@5 = " + P5);

        }
        System.out.println(averageP5/count);
        br.close();
        output.close();
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
        int count = 0;//COUNTING FOR AVERAGE P@5
        Double averageP5 = 0.0;

        BufferedReader runIn = new BufferedReader(new FileReader(
                    Paths.get(runFile).toFile()));

        HashMap<Integer, ArrayList<String>> runMap = new HashMap<Integer, ArrayList<String>>();
        line = "";
        // Fill runMap with each profiles runs
        JSONObject runResponse = null;
        JSONParser parser2 = new JSONParser();
        while((line = runIn.readLine()) != null){
            
            try 
            {
                runResponse = (JSONObject) parser2.parse(line);
                //line2 = br2.readLine();//The run line to check against results
                //runResponse = (JSONObject) parser2.parse(line2);
            } 
            catch (ParseException pe) 
            {
                System.err.println("Error: could not parse JSON response:");
                System.out.println(line);
                System.exit(1);
            }
            
            int id = ((Long)runResponse.get("id")).intValue();
            ArrayList<String> rankedAttractions = runMap.get(id);
            if(rankedAttractions == null){
                rankedAttractions = new ArrayList<String>();
            }
            
            JSONObject o = (JSONObject)runResponse.get("body");
            
            JSONArray candidates = (JSONArray) o.get("suggestions");
            for(int i =  0; i < candidates.size(); i++)
            {
                String trec_id = candidates.get(i).toString();
                rankedAttractions.add(trec_id);
            }

            runMap.put(id, rankedAttractions);
        }

        runIn.close();
        line = "";
        try
        {
            br = new BufferedReader(new FileReader(Paths.get("../DataFiles/SurveyResults.json").toFile()));
            //br2 =  new BufferedReader(new FileReader(Paths.get(runFile).toFile()));
            //buffered writer for output file
            output = new PrintWriter(new FileWriter("evalOutput.txt"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        JSONParser parser = new JSONParser();
        
        JSONObject response = null;
        //JSONObject runResponse = null;
        while((line = br.readLine())!=null)
        {
            count++;
            results.clear();
            //JSON parsing for response
            try 
            {
                response = (JSONObject) parser.parse(line);
                //line2 = br2.readLine();//The run line to check against results
                //runResponse = (JSONObject) parser2.parse(line2);
            } 
            catch (ParseException pe) 
            {
                System.err.println("Error: could not parse JSON response:");
                System.out.println(line);
                System.exit(1);
            }
            catch (NullPointerException e) 
            {
                System.err.println("Error: null pointer" + e);
            }
            //obtain the User ID
            user_ID = ((Long)response.get("id")).intValue();
            //obtain JSON array of result TREC ID's
            JSONArray resultIDs = (JSONArray) response.get("results");
            for(int i =  0; i < resultIDs.size(); i++)
            {
                String trec_id = resultIDs.get(i).toString();
                results.add(trec_id);
            }

            //JSONObject body = (JSONObject) runResponse.get("body");
            //JSONArray runIDs = (JSONArray) body.get("suggestions");
            ArrayList<String> runIDs = runMap.get(user_ID);
            P5 = 1.0;
            for(int i = 0; i < 5; i++)
            {
                String runID = runIDs.get(i);
                if(!results.contains(runID))
                {
                    P5 = P5 - .2;
                }
            }
            averageP5 += P5;
            output.println(user_ID + ": P@5 = " + P5);

        }
        System.out.println(averageP5/count);
        br.close();
        output.close();
    }
}