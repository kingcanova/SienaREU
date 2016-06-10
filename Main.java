import java.util.*;
import java.nio.*;
import java.nio.file.*;
import java.io.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
/**
 * This class is the Main Program of the Contextual Suggestion TREC Track for Siena College. 
 * 
 * @author Tristan Canova, Dan Carpenter, Neil Devine
 * @version 6/9/16
 */
public class Main
{
    /**
     * This member is a map which maps profile IDs to their respective profile; to mimic 2015 results, use a Hashtable for this member.
     */ 
    public static TreeMap<Integer, Profile> profiles = new TreeMap<Integer, Profile>();
    /**
     * This method builds each individual profile and puts them in 'profiles'. It should be
     * noted that each profile is taken from the 'batch_requests.json' file (from TREC).
     */
    private static void buildProfiles() throws IOException
    {
        BufferedReader in = null;
        try{
            in = new BufferedReader(new FileReader(Paths.get("../DataFiles/batch_requests.json").toFile()));
        }catch(FileNotFoundException ex){
            System.err.println("Could not find batch_requests.json in DataFiles directory");
            return;
        }
        String line = "";
        while((line = in.readLine()) != null){ // For each line in batch_requests
            if(line.equals("")){
                break;
            }
            Profile profile = new Profile(line);
            profiles.put(profile.user_ID, profile);
        }

        Set<Integer> people = profiles.keySet();
        for(Integer num : people){
            Profile person = profiles.get(num);
            Set<String> keys = person.cat_occurance.keySet();
            for(String cat : keys){
                person.cat_count.put(cat, (person.cat_count.get(cat)/person.cat_occurance.get(cat)));
            }
        }
        in.close();
    }

    /**
     * This method is the full program which orders 30 suggestions for each profile and outputs
     * them to a JSON file.
     * @param args Command Line Arguments
     */
    public static void main(String args[]) throws IOException
    {
        /**
         * PrintWriter for final output file. This file will contain each profiles rated attractions in order from left to right, with the leftmost
         * attractions being the most likely to be relevant, while the rightmost attractions are the least likely to be relevant.
         */
        PrintWriter pw = new PrintWriter("testOutput.json"); 

        /**
         * File IO for UneccessaryCats.txt file. This file contains a list of unnecessary categories. These categories are then ignored by
         * the scoring algorithm.
         */
        Scanner in = null;
        try{
            in = new Scanner(new File("../DataFiles/UneccessaryCats.txt"));
        }catch(FileNotFoundException ex){
            System.err.println("UneccessaryCats.txt was not found in the DataFiles directory");
            return;
        }
        ArrayList<String> ignoredCats = new ArrayList<String>();

        while (in.hasNextLine()){
            ignoredCats.add(in.nextLine());
        }

        /**
         * File IO for serialized profiles.dat file. profiles.dat contains a saved version of the profiles TreeMap data member (of this class). If this
         * file already exists, the program loads it and uses the saved profiles used previously. If it does not, then the program recreates the profiles
         * TreeMap object. One would want to recreate the profiles object so that they may re-access the API's and retrieve either more categories or store
         * more data to be manipulated in the scoring algorithm.
         */

        File saveFile = null;
        try{
            saveFile = new File("../DataFiles/profiles.dat");
        }catch(Exception ex){
            System.err.println("Could not find profiles.dat in DataFiles directory");
            return;
        }

        boolean serialized = saveFile.exists();

        if(serialized){ // If the profiles object exists, use it
            try(FileInputStream s = new FileInputStream("../DataFiles/profiles.dat")) {
                ObjectInputStream s2 = new ObjectInputStream(s);
                profiles = (TreeMap<Integer, Profile>)s2.readObject();
            }catch(Exception e){
                System.err.println("error finding profiles.dat");
            }
        }else{ // If it doesn't, recreate it
            buildProfiles();
            try(FileOutputStream f = new FileOutputStream("../DataFiles/profiles.dat")){
                ObjectOutputStream f2 = new ObjectOutputStream(f);
                f2.writeObject(profiles);
                f2.flush();
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        Set<Integer> profileIDs = profiles.navigableKeySet(); // A navigable (sorted) set of profile IDs
        for(Integer id : profileIDs) // For each Profile ID
        {
            Profile person = profiles.get(id); // Get an individual Profile Object
            ArrayList<Attraction> attractions = person.candidates; // Get the list of all of its candidates (attractions to rate)

            /**
             * Main Scoring Algorithm: this algorithm scores each of the Profile's candidates
             */
            for (Attraction a : attractions)
            {
                boolean hasCategories = false; // Whether this attraction ('a') has any categories
                for(String cat : a.categories)
                {
                    hasCategories = true;
                    if(person.cat_count.get(cat) != null && !ignoredCats.contains(cat))
                    {
                        a.score += person.cat_count.get(cat);
                        a.count += 1;
                    }
                    
                    /**
                     * NOTES:
                     * Fast food and young men
                     * Gyms and younger people
                     */
                    
                    
                    // Messing around with the scoring algorithm...
                    //                     if(cat.equals("bar")){
                    //                         a.score = -1.0;
                    //                     }
                    //                     else if(cat.equals("museum") || cat.equals("park")){
                    //                         a.score= a.score * 2;
                    //                     }
                    //                     else if(cat.equals("meal_takeaway")){
                    //                         a.score += 5;
                    //                     }
                    //                     else if(cat.equals("lodging")){
                    //                         a.score = a.score / 2;
                    //                     }

                }

                if(a.count > 0)
                    a.score = a.score / a.count;
                else if(!hasCategories) // Place the attractions that don't have categories at the very end of the suggested list
                    a.score = -Double.MAX_VALUE;
            }
            /**
             * End of scoring algorithm
             */

            Collections.sort(attractions); // Sort the list of attractions by their score.

            // Print out the sorted list of candidates to the console.
            for(int i = 0; i<attractions.size(); i++){
                System.out.printf("%2d) %-35s %5.2f\n",
                    i+1, attractions.get(i).name, attractions.get(i).score);
            }
            System.out.println("Sorted Results:     " + person.user_ID);

            /**
             * Writing to the output file
             */
            pw.print("{\"body\": {\"suggestions\": [");

            for (int k=0; k<attractions.size(); k++){
                int currID = attractions.get(k).id;
                String attrID = "00000000" + currID;
                attrID = attrID.substring(attrID.length()-8);
                pw.print("\"TRECCS-" + attrID + "-" + person.contextID + "\"");
                if(k != attractions.size() - 1) {pw.print(",");}
            }
            pw.print("]}, \"groupid\": \"Siena_SUCCESS\", \"id\": " + id +
                ", \"runid\": \"SCIAIrunA\"}");
            pw.println();
        }

        // Close IO objects
        in.close();
        pw.close();
    }
}
