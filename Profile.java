import java.util.*;
import java.io.*;
import javax.swing.*;
import java.nio.*;
import java.nio.file.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
/**
 * Write a description of class Profile here.
 * 
 * @author Tristan Canova, Neil Divine, Dan Carpenter
 * @version 6/1/16
 */
public class Profile implements Serializable
{
    // TREC / Attraction ID -> Attraction Name
    transient static HashMap<Integer, String> collection;

    // Context ID -> Coordinates
    transient static HashMap<Integer, String> coordinates;

    // Attraction ID -> Attraction Object
    transient static HashMap<Integer, Attraction> attrs = new HashMap<Integer, Attraction>();
    static{
        collection = new HashMap<Integer, String>();
        BufferedReader in = null;
        try{
            in = new BufferedReader(new FileReader(Paths.get("collection_2015.csv").toFile()));
            String line = "";
            //int lineNum = 1;
            while((line = in.readLine()) != null){
                String[] content = split(line, 4);
                //System.out.println(lineNum);
                String[] trecID = content[0].split("-");
                int id = Integer.parseInt(trecID[1]);
                String name = content[3];
                collection.put(id, name);
                //lineNum++;
            }
            in.close();
        }catch(IOException e){
            e.printStackTrace();
        }

        coordinates = new HashMap<Integer, String>();
        BufferedReader in2 = null;
        try{
            in2 = new BufferedReader(new FileReader(Paths.get("contexts2015coordinates.csv").toFile()));
            in2.readLine();
            String line = "";
            while((line = in2.readLine()) != null){
                String[] content = line.split(",");
                int contextID = Integer.parseInt(content[0]);
                String coords = content[1] + "," + content[2];
                coordinates.put(contextID, coords);
            }
            in2.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    private int user_ID;
    private int contextID;
    private Double age;
    //private ArrayList<Integer> attractions = new ArrayList<Integer>();
    //maps each attraction category to its specific score
    private HashMap<String, Double> cat_count = new HashMap<String, Double>();
    //maps each attraction category to the amount of times its been rated
    private HashMap<String, Integer> cat_occurance = new HashMap<String, Integer>();

    public ArrayList<Attraction> candidates = new ArrayList<Attraction>();

    public ArrayList<Attraction> prefs = new ArrayList<Attraction>();

    public Profile(String line)
    {
        JSONParser parser = new JSONParser();
        JSONObject response = null;
        try 
        {
            response = (JSONObject) parser.parse(line);
        } catch (ParseException pe) {
            System.err.println("Error: could not parse JSON response:");
            System.out.println(line);
            System.exit(1);
        }
        catch (NullPointerException e) {
            System.err.println("Error: null pointer" + e);
        }
        //obtain JSON array of TREC's suggested attractions for this individual
        JSONArray cands = (JSONArray) response.get("candidates");
        System.out.println("___________Candidates__________\n");
        for(int i=0;i<cands.size();i++)
        {
            //split TREC ID to place the attraction ID's into a person's candidate list
            String trec_id = cands.get(i).toString();
            String[] elements = trec_id.split("-");
            int attrID = Integer.parseInt(elements[1]);
            int contextID = Integer.parseInt(elements[2]);
            String name = collection.get(attrID);
            String coords = coordinates.get(contextID);
            Attraction attr;
            if(attrs.get(attrID) == null){
                attr = new Attraction(name, coords);
                attr.id = attrID;
                attrs.put(attrID, attr);
            }else{
                attr = new Attraction(attrs.get(attrID));
            }

            System.out.println(attr);
            candidates.add(attr);
            //this.attractions.add(Integer.parseInt(elements[1]));
        }
        //obtain all other information from JSON response and store in instance variables
        JSONObject body = (JSONObject) response.get("body");//This specifies where we are looking for the information
        this.user_ID = ((Long)response.get("id")).intValue();//This gets the ID number at the end of the string
        System.out.println("For Person: " + user_ID +  "\n");
        //             String group = body.get("group").toString();
        //             String season = body.get("season").toString();
        //             String trip_type = body.get("trip_type").toString();
        //             String duration = body.get("duration").toString();
        JSONObject location = (JSONObject) body.get("location");//Changing from body to location for info searching
        this.contextID = ((Long)location.get("id")).intValue();
        JSONObject individual = (JSONObject) body.get("person");
        //             String gender = individual.get("gender").toString();
        this.age = (Double)individual.get("age");

        //Preferences is a JSON Array containing the list of attractions a profile rated
        //This cycles through the array, grabbing the attraction ID and rating, also
        //obtaining the tags and categories for an attraction and assigning them scores
        JSONArray preferences = (JSONArray) individual.get("preferences");
        System.out.println("__________Preferences____________\n");
        for(int i = 0; i<preferences.size(); i++)
        {
            //obtain TREC ID and parse out the attraction id and rating, store in instance variables
            JSONObject pref = (JSONObject)preferences.get(i);
            String trec_id = pref.get("documentId").toString();
            String[] elem = trec_id.split("-");
            int att_id = Integer.parseInt(elem[1]);
            int contextID = Integer.parseInt(elem[2]);
            int rating = Integer.parseInt(pref.get("rating").toString());
            //obtain JSON Array of tags(reasons why person liked attraction)
            //we will treat these as more categories and merge them with the API returned categories
            JSONArray tags = (JSONArray) pref.get("tags");

            // FROM OLD CODE
            //retrieve the suggestion corresponding to the attr id 
            //Attraction curr = Main.attrs.get(att_id);//THIS WILL BE CHANGED TO MAIN.ATTR.GET(att_id);

            String name = collection.get(att_id);
            String coords = coordinates.get(contextID);
            //Attraction curr = new Attraction(name, coords);

            Attraction curr;
            if(attrs.get(att_id) == null){
                curr = new Attraction(name, coords);
                curr.id = att_id;
                attrs.put(att_id, curr);
            }else{
                curr = new Attraction(attrs.get(att_id));
            }

            System.out.println(curr);
            prefs.add(curr);

            double[] scores = new double[]{-4.0, -2.0, 1.0, 2.0, 4.0};
            //treat tags of examples as more categories,and assign them a score based on rating
            if (tags != null)
            {
                for (Object t : (JSONArray)tags)
                {
                    String tag = t.toString();
                    //if category/tag isnt in the count(score) table yet, 
                    //put it in the count and occurance tables
                    if (this.cat_count.get(tag) == null)
                    {
                        this.cat_count.put(tag, 0.0);
                        this.cat_occurance.put(tag, 0);
                    }
                    //if category is given a rating, add the appropriate score to its value in the
                    //score table. Also add 1 to its occurance value. 
                    if(rating != -1)
                    {
                        this.cat_count.put(tag, this.cat_count.get(tag) + scores[rating]);
                        this.cat_occurance.put(tag, this.cat_occurance.get(tag) +1);
                    }
                }      
            }
            //repeat the same process as above, using the same tables. However, instead of
            //using the tags, use the categories returned by the API's
            if (curr != null)
            {
                for (String cat : curr.categories)
                {
                    if (this.cat_count.get(cat) == null)
                    {
                        this.cat_count.put(cat, 0.0);
                        this.cat_occurance.put(cat, 0);
                    }
                    if(rating != -1)
                    {
                        this.cat_count.put(cat, this.cat_count.get(cat) + scores[rating]);
                        this.cat_occurance.put(cat, this.cat_occurance.get(cat) +1);
                    }
                }      
            }
        }         
        System.out.println();
    }

    protected int getUser_ID()
    {
        return this.user_ID;
    }

    protected int getContextID()
    {
        return this.contextID;
    }

    protected Double getAge()
    {
        return this.age;
    }

    protected HashMap<String, Double> getCat_count()
    {
        return this.cat_count;
    }

    protected HashMap<String, Integer> getCat_occurance()
    {
        return this.cat_occurance;
    }

    /**
     * Point of Interest, or Attraction. 
     * Each attraction has a name, description, url, and ID number
     * These POIs will later be ranked based on someone's profile 
     * @version May
     */
    public static String[] split(String csvline, int len)
    {
        boolean inquote = false;
        String[] vals = new String[len];
        Arrays.fill(vals, "");
        int valindex = 0;
        for (int i=0; i<csvline.length(); i++)
        {
            if (csvline.charAt(i) == '"')
                inquote = !inquote;
            if (csvline.charAt(i) == ',' && !inquote)
            {
                valindex++;
                continue;
            }
            vals[valindex] += csvline.charAt(i);
        }
        return vals;
    }
}