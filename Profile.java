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
public class Profile
{
    private int user_ID;
    private int contextID;
    private Double age;
    private ArrayList<Integer> attractions = new ArrayList<Integer>();
    //maps each attraction category to its specific score
    private HashMap<String, Double> cat_count = new HashMap<String, Double>();
    //maps each attraction category to the amount of times its been rated
    private HashMap<String, Integer> cat_occurance = new HashMap<String, Integer>();

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
        for(int i=0;i<cands.size();i++)
        {
            //split TREC ID to place the attraction ID's into a person's candidate list
            String trec_id = cands.get(i).toString();
            String[] elements = trec_id.split("-");
            this.attractions.add(Integer.parseInt(elements[1]));
        }
        //obtain all other information from JSON response and store in instance variables
        JSONObject body = (JSONObject) response.get("body");//This specifies where we are looking for the information
        this.user_ID = ((Long)response.get("id")).intValue();//This gets the ID number at the end of the string
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
        for(int i = 0; i<preferences.size(); i++)
        {
            //obtain TREC ID and parse out the attraction id and rating, store in instance variables
            JSONObject pref = (JSONObject)preferences.get(i);
            String trec_id = pref.get("documentId").toString();
            String[] elem = trec_id.split("-");
            int att_id = Integer.parseInt(elem[1]);
            int rating = Integer.parseInt(pref.get("rating").toString());
            //obtain JSON Array of tags(reasons why person liked attraction)
            //we will treat these as more categories and merge them with the API returned categories
            JSONArray tags = (JSONArray) pref.get("tags");

            //retrieve the suggestion corresponding to the attr id 
            Attraction curr = Main.attrs.get(att_id);//THIS WILL BE CHANGED TO MAIN.ATTR.GET(att_id);
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

    protected ArrayList<Integer> getAttractions()
    {
        return this.attractions;
    }

    protected HashMap<String, Double> getCat_count()
    {
        return this.cat_count;
    }

    protected HashMap<String, Integer> getCat_occurance()
    {
        return this.cat_occurance;
    }
}