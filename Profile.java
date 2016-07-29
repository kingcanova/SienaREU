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
 * This Class represents a user profile as described in 'batch_requests.json'. A profile is therefore our user model, storing and manipulating 
 * relevant information to later determine with which attractions this user would be most interested. It should also be noted that some File I/O
 * is performed statically within this file, so that all Profiles have all the information required.
 * 
 * @author Tristan Canova, Neil Divine, Dan Carpenter
 * @version 6/9/16
 */
public class Profile implements Serializable
{
    // TREC / Attraction ID -> Attraction Name
    transient static HashMap<Integer, String> collection;

    // Context ID -> Context (location and coordinates)
    transient static HashMap<Integer, Context> contexts;

    // Attraction ID -> Attraction Object
    // Either initialized as empty or loaded from a serialized file in Main.main()
    transient static HashMap<Integer, Attraction> attrs;

    // List of all categories (Note: this list was hand picked)
    transient static ArrayList<String> categories = new ArrayList<>();
    static//Static initializer block that fills the Hashmaps collection and coordinates 
    {
        collection = new HashMap<Integer, String>();
        BufferedReader in = null;
        try{
            in = new BufferedReader(new FileReader(Paths.get("../DataFiles/collection_2015.csv").toFile()));//The collection file given to us by Trec VERY LARGE 
            //contains attraction names ^
            String line = "";
            while((line = in.readLine()) != null){
                String[] content = split(line, 4);
                String[] trecID = content[0].split("-");
                int id = Integer.parseInt(trecID[1]);
                String name = content[3];
                collection.put(id, name);//Puts the attraction ID and name into the collection Hashmap
            }
            in.close();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }

        contexts = new HashMap<Integer, Context>();
        BufferedReader in2 = null;
        try{
            /**
             * Please Note: contexts2015.csv is a *combination* of contexts2015.csv (The original) and contexts2015coordinates.csv. The original
             * contexts2015.csv contained context IDs, state, and city names, while contexts2015coordinates.csv contained context IDs, latitude, 
             * and longitude information. We merely combined the two files for convenience :)
             */
            in2 = new BufferedReader(new FileReader(Paths.get("../DataFiles/contexts2015.csv").toFile()));//Given by Trec gives the context coordinates
            in2.readLine(); // Consume the first line
            String line = "";
            while((line = in2.readLine()) != null){
                String[] content = line.split(",");
                int contextID = Integer.parseInt(content[0]);
                String loc = content[1] + ", " + content[2];
                String coords = content[3] + "," + content[4];
                String geoID = content[5];
                Context context = new Context(loc, coords, geoID);
                contexts.put(contextID, context);// Associates the Context object with its context ID
            }
            in2.close();
        }catch(IOException e){
            e.printStackTrace();
        }

        BufferedReader in3 = null;
        try{
            in3 = new BufferedReader(new FileReader(Paths.get("../DataFiles/categories.txt").toFile()));
            String line = "";
            while((line = in3.readLine()) != null){
                categories.add(line.trim());
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    public int user_ID;
    public int contextID;

    //maps each attraction category to its specific score
    public HashMap<String, Double> cat_count = new HashMap<String, Double>();
    //maps each attraction category to the amount of times its been rated
    public HashMap<String, Integer> cat_occurance = new HashMap<String, Integer>();

    public ArrayList<Attraction> candidates = new ArrayList<Attraction>();//An arraylist to be filled with the candidate attractions

    public ArrayList<Attraction> prefs = new ArrayList<Attraction>();//An arraylist to be filled with a subjects preferences 

    // Data relevant to their profile
    public char gender;
    public String group, season, trip_type, duration;
    public Double age;
    /**
     * This is the Profile class constructor. It takes in a line of information taken from a file given by Trec and we parse out all the useful
     * information and store it in a meaningful and useful way
     * @param line A single line from "batch_requests.json" which represents one complete profile
     */
    public Profile(String line, boolean query)
    {
        JSONParser parser = new JSONParser();// Set up JSON parser because its easier to use JSON objects to get info from the line
        JSONObject response = null;
        try 
        {
            response = (JSONObject) parser.parse(line);
        } 
        catch (ParseException pe) 
        {
            System.err.println("Error: could not parse JSON response:");
            System.out.println(line);
        }
        catch (NullPointerException e)
        {
            System.err.println("Error: null pointer" + e);
        }

        // Obtain JSON array of TREC's suggested "candidate" attractions for this individual
        JSONArray cands = (JSONArray) response.get("candidates");
        System.out.println("___________Candidates__________\n");
        for(int i = 0; i < cands.size(); i++)
        {
            //split TREC ID to place the attraction ID's into a person's candidate list
            String trec_id = cands.get(i).toString();
            String[] elements = trec_id.split("-");
            int attrID = Integer.parseInt(elements[1]);
            int contextID = Integer.parseInt(elements[2]);
            String name = collection.get(attrID).split(" - ")[0].replace("\"", "").split(" \\| ")[0];
            Context context = contexts.get(contextID);
            Attraction attr;
            boolean copied = false; // Whether the current attraction object used the copy constructor (this will be false the first time the object is created)
            //if the attraction is null, try again

            if(query){ // Potentially query the APIs again
                if(attrs.get(attrID) == null) { // Make the attraction object for the first time
                    attr = new Attraction(name, context);
                    attr.id = attrID;
                    attrs.put(attrID, attr);
                }
                else if (!attrs.get(attrID).infoFound) { // If we may have gotten some info before but nothing from TripAdvisor, try again and merge results
                    String atName = attrs.get(attrID).name;
                    if(atName.contains(" html ") || atName.contains(" php ") || atName.contains(" com ") || atName.contains(" org ") || atName.contains(".com")){
                        copied = true;
                        attr = new Attraction(attrs.get(attrID));
                    }
                    else{
                        ArrayList<String> oldAttr = attrs.get(attrID).categories;
                        attr = new Attraction(name, context);
                        attr.id = attrID;
                        for (String s : attr.categories) { // Merge old categories into the new attraction object
                            if (!attr.categories.contains(s)) attr.categories.add(s);
                        }
                        attrs.put(attrID, attr);
                    }
                }
                else { // We already have info for this attraction
                    copied = true;
                    attr = new Attraction(attrs.get(attrID));
                }
            }else{ // Don't query the APIs; merely take the attraction object from 'attrs'
                copied = true;
                attr = new Attraction(attrs.get(attrID));
            }

            if(!copied){ // If the attraction object has been created for the first time
                // Make the categories created by the APIs conform to our predefined categories
                // The attractions categories will eventually become 'newCategories'

                HashSet<String> newCategories = new HashSet<String>();
                for (String cat : attr.categories)
                {
                    cat = cat.trim().toLowerCase();
                    double minSimilarity = Double.MAX_VALUE;
                    String finalCat = "";

                    // Search through all of our predefined categories and take the most similar (minSimilarity) category as its final category (finalCat)
                    for(String realCat : categories){
                        realCat = realCat.trim().toLowerCase();
                        String a, b;
                        if(realCat.length() > cat.length()){
                            b = realCat;
                            a = cat;
                        }else{
                            a = realCat;
                            b = cat;
                        }
                        double similarity = Lev.categorySimilarity(a, b);

                        if(similarity <= 20 && similarity < minSimilarity){
                            minSimilarity = similarity;
                            finalCat = realCat;
                        }
                    }

                    if(!finalCat.equals("")){
                        newCategories.add(finalCat);
                    }
                }

                // Merely copy newCategories to the attractions categories (resetting it)
                attr.categories = new ArrayList<String>(Arrays.asList(newCategories.toArray(new String[newCategories.size()])));
            }

            System.out.println(attr);
            candidates.add(attr);
        }

        //obtain all other information from JSON response and store in instance variables
        JSONObject body = (JSONObject) response.get("body");//This specifies where we are looking for the information
        this.user_ID = ((Long)response.get("id")).intValue();//This gets the ID number at the end of the string
        System.out.println("For Person: " + user_ID +  "\n");
        JSONObject individual = (JSONObject) body.get("person");

        // Obtaining and storing relevant information about the User
        if(body.get("group") != null){
            group = body.get("group").toString();
        }
        if(body.get("season") != null){
            season = body.get("season").toString();
        }
        if(body.get("trip_type") != null){
            trip_type = body.get("trip_type").toString();
        }
        if(body.get("duration") != null){
            duration = body.get("duration").toString();
        }
        if(individual.get("gender") != null){
            gender = Character.toUpperCase(individual.get("gender").toString().charAt(0));
        }
        if(individual.get("age") != null){
            age = (Double)individual.get("age");
        }
        JSONObject location = (JSONObject) body.get("location");//Changing from body to location for info searching
        this.contextID = ((Long)location.get("id")).intValue();

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

            String name = collection.get(att_id).split(" - ")[0].replace("\"", "").split(" \\| ")[0];
            Context context = contexts.get(contextID);

            Attraction curr;
            boolean copied = false;
            //if the attraction is null or there was no info found on it, try again

            if(query){
                if(attrs.get(att_id) == null)
                {
                    curr = new Attraction(name, context);  //query APIs to get info
                    curr.id = att_id;
                    attrs.put(att_id, curr);
                }
                else if (!attrs.get(att_id).infoFound) { //if we may have gotten some info before but nothing from TripAdvisor, try again and merge results
                    String atName = attrs.get(att_id).name;
                    if(atName.contains(" html ") || atName.contains(" php ") || atName.contains(" com ") || atName.contains(" org ") || atName.contains(".com")){
                        copied = true;
                        curr = new Attraction(attrs.get(att_id));
                    }
                    else{
                        ArrayList<String> oldAttr = attrs.get(att_id).categories;
                        curr = new Attraction(name, context);
                        curr.id = att_id;
                        for (String s : curr.categories) {
                            if (!curr.categories.contains(s)) curr.categories.add(s);
                        }
                    }
                }
                else { //we already have info for this attraction
                    copied = true;
                    curr = new Attraction(attrs.get(att_id));
                }
            }else{
                copied = true;
                curr = new Attraction(attrs.get(att_id));
            }

            prefs.add(curr);//Adds the current attraction 
            double[] scores = new double[]{-4.0, -2.0, 1.0, 2.0, 4.0};
            //treat tags of examples as more categories,and assign them a score based on rating

            HashSet<String> newCategories = new HashSet<String>();
            if (tags != null) // For this preferences tags
            {
                for (Object t : (JSONArray)tags)
                {
                    // Update cat_count and cat_occurance with the tags of this preference
                    // Note: We also make the tags conform to our predefined categories (see above for more information on this)

                    String tag = t.toString().trim().toLowerCase();
                    double minSimilarity = Double.MAX_VALUE;
                    String finalCat = "";
                    for(String realCat : categories){
                        realCat = realCat.trim().toLowerCase();
                        String a, b;
                        if(realCat.length() > tag.length()){
                            b = realCat;
                            a = tag;
                        }else{
                            a = realCat;
                            b = tag;
                        }
                        double similarity = Lev.categorySimilarity(a, b);

                        if(similarity <= 20 && similarity < minSimilarity){
                            minSimilarity = similarity;
                            finalCat = realCat;
                        }
                    }

                    // Updating newCategories, cat_count, and cat_occurance
                    if(!finalCat.equals("")){
                        newCategories.add(finalCat);

                        // Set this category's count and occurance if it does not exist
                        if (this.cat_count.get(finalCat) == null)
                        {
                            this.cat_count.put(finalCat, 0.0);
                            this.cat_occurance.put(finalCat, 0);
                        }

                        // If this category has a rating, set cat_count and cat_occurance accordingly
                        if(rating != -1)
                        {
                            this.cat_count.put(finalCat, this.cat_count.get(finalCat) + scores[rating]);
                            this.cat_occurance.put(finalCat, this.cat_occurance.get(finalCat) +1);
                        }
                    }
                }      
            }

            if (curr != null) // This preference's categories (those returned by the APIs)
            {
                for (String cat : curr.categories)
                {
                    // Conforming Data (for the third time, but this time with the preference's categories)

                    cat = cat.trim().toLowerCase();
                    double minSimilarity = Double.MAX_VALUE;
                    String finalCat = "";
                    for(String realCat : categories){
                        realCat = realCat.trim().toLowerCase();
                        String a, b;
                        if(realCat.length() > cat.length()){
                            b = realCat;
                            a = cat;
                        }else{
                            a = realCat;
                            b = cat;
                        }
                        double similarity = Lev.categorySimilarity(a, b);

                        if(similarity <= 20 && similarity < minSimilarity){
                            minSimilarity = similarity;
                            finalCat = realCat;
                        }
                    }

                    // Updating newCategories, cat_count, and cat_occurance
                    if(!finalCat.equals("")){
                        newCategories.add(finalCat);

                        // Set this category's count and occurance if it does not exist
                        if (this.cat_count.get(finalCat) == null)
                        {
                            this.cat_count.put(finalCat, 0.0);
                            this.cat_occurance.put(finalCat, 0);
                        }

                        // If this category has a rating, set cat_count and cat_occurance accordingly
                        if(rating != -1)
                        {
                            this.cat_count.put(finalCat, this.cat_count.get(finalCat) + scores[rating]);
                            this.cat_occurance.put(finalCat, this.cat_occurance.get(finalCat) +1);
                        }
                    }
                }      
            }

            // Set this preference's categories to newCategories (as done previously with candidates)
            if(!copied)
                curr.categories = new ArrayList<String>(Arrays.asList(newCategories.toArray(new String[newCategories.size()])));

            System.out.println(curr);
        }         
        System.out.println();
    }

    /**
     * This method splits a line of a .csv file and returns a string array (with length 'len' of that line.
     * Note: the reason it is not the same as someString.Split(",") because this method ignores commas within quotes (strings within the string)
     * @param csvline The line to be split
     * @param len The length of the array to be returned
     * @return A string array of 'csvline' split by commas
     * @version May 2015
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