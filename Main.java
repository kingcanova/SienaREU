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
 * @author Tristan Canova, Dan Carpenter, Neil Devine, Kevin Danaher
 * @version 6/9/16
 */
public class Main
{
    /**
     * This member is a map which maps profile IDs to their respective profile; to mimic 2015 results, use a Hashtable for this member.
     */ 
    public static TreeMap<Integer, Profile> profiles = new TreeMap<>();
    /**
     * This method builds each individual profile and puts them in 'profiles'. It should be
     * noted that each profile is taken from the 'batch_requests.json' file (from TREC).
     */
    private static void buildProfiles(boolean query) throws IOException
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
            Profile profile = new Profile(line, query);
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
     * them to a JSON file. In addition
     * 
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
        Set<String> ignoredCats = new HashSet<>();

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
            saveFile = new File("../DataFiles/attractions.dat");
        }catch(Exception ex){
            System.err.println("Could not find attractions.dat in DataFiles directory");
            return;
        }

        boolean serialized = saveFile.exists();

        if(serialized){ // If attrs exists, build upon it
            try(FileInputStream s = new FileInputStream("../DataFiles/attractions.dat")) {
                ObjectInputStream s2 = new ObjectInputStream(s);
                Profile.attrs = (HashMap<Integer, Attraction>)s2.readObject();
            }catch(Exception e){
                System.err.println("Error finding attractions.dat");
            }
        }else{
            Profile.attrs = new HashMap<Integer, Attraction>();
        }

        buildProfiles(true);
        try(FileOutputStream f = new FileOutputStream("../DataFiles/attractions.dat")){ //writing attrs
            ObjectOutputStream f2 = new ObjectOutputStream(f);
            f2.writeObject(Profile.attrs);
            f2.flush();
        }catch(Exception e){
            e.printStackTrace();
        }

        Set<Integer> profileIDs = profiles.navigableKeySet(); // A navigable (sorted) set of profile IDs
        int reviewsFound = 0;
        for(Integer id : profileIDs) // For each Profile ID
        {
            Profile person = profiles.get(id); // Get an individual Profile Object
            ArrayList<Attraction> attractions = person.candidates; // Get the list of all of its candidates (attractions to rate)

            /**
             * Main Scoring Algorithm: this algorithm scores each of the Profile's candidates
             * NOTE: All blocks with the comment "VARIABLE" are main factors to our score, which may be modified
             * to optimize the final score over multiple test cases / data sets.
             */
            for (Attraction a : attractions)
            {
                a.score = 0.0; // Reset the attraction score (in case it has been recently set)
                boolean hasCategories = false; // Whether this attraction ('a') has any categories
                for(String cat : a.categories)
                {
                    hasCategories = true;

                    // Whether this category has a count and it is not an ignored category
                    boolean contains = person.cat_count.get(cat) != null && !ignoredCats.contains(cat);

                    if(contains){
                        a.score += (person.cat_count.get(cat) * person.cat_occurance.get(cat)) / (person.cat_occurance.get(cat) + 1);
                        a.count += 1;
                    }

                    // VARIABLE
                    if(a.certificate && contains && person.cat_count.get(cat) >= 1.5){ // GOOD (1 / 1.5)
                        a.score += 3;
                    }

                    // VARIABLE
                    if(a.numReviews > 1000 && a.rating >= 4.0 && contains && person.cat_count.get(cat) >= 2.5){ // GOOD (1000, 4.0, 2.5)
                        a.score += a.rating*0.8;
                    }

                    if(cat.equals("pizza")){ // Obligatory Pizza Scoring
                        //a.score += 3;
                    }
                }

                // VARIABLE
                if(a.count > 0){
                    a.score = a.score / a.count;
                }
                else if(!hasCategories){ // Place the attractions that don't have categories at the very end of the suggested list
                    //a.score = -Double.MAX_VALUE;
                }

                // VARIABLE
                if(a.rating > 0.0){ // GOOD
                    a.score += a.rating;
                }

                // VARIABLE
                if(a.certificate){
                    //a.score += 2;
                }

                // VARIABLE
                if(a.numReviews > 1000 && a.rating >= 4.0){
                    //a.score += a.rating;
                }

                // VARIABLE
                if(a.seasons != null && !a.seasons.equals("")){ // Scoring for matching seasons
                    String[] seas = a.seasons.split("\n");
                    int[] seasonRatings = new int[4];
                    String[] seasonAr = new String[]{"Spring", "Summer", "Autumn", "Winter"};
                    HashMap<Integer, String> seasonMap = new HashMap<Integer, String>();
                    for(int i = 0; i < 4; i++){
                        String sub = seas[i].substring(0,seas[i].length()-8).substring(7);
                        seasonRatings[i] = Integer.parseInt(sub.replace(",",""));
                        seasonMap.put(seasonRatings[i], seasonAr[i]);
                    }
                    Arrays.sort(seasonRatings);
                    String s = person.season;

                    if(s != null && (s.equals(seasonMap.get(seasonRatings[2])) || s.equals(seasonMap.get(seasonRatings[3])))){
                        //a.score += 1;
                    }
                }

                // VARIABLE
                if(a.travelerTypes != null && !a.travelerTypes.equals("")){ // Scoring for matching Trip Types
                    String[] groups = a.travelerTypes.split("\n");
                    int[] groupRatings = new int[5];
                    String[] groupAr = new String[]{"Family", "Alone", "Friends", "Other", "Friends"};
                    HashMap<Integer, String> groupMap = new HashMap<Integer, String>();
                    for(int i = 0; i < 5; i++){
                        String sub = groups[i].substring(0,groups[i].length()-8).substring(7);
                        groupRatings[i] = Integer.parseInt(sub.replace(",", ""));
                        groupMap.put(groupRatings[i], groupAr[i]);
                    }
                    Arrays.sort(groupRatings);

                    String g = person.group;
                    if(g != null && (g.equals(groupMap.get(groupRatings[4])) || g.equals(groupMap.get(groupRatings[3])))){
                        a.score += 1.5;
                    }
                }

                // Penalty for unrealistic attractions (those whose names are most likely just URLs of some webpage)
                String name = a.name;
                if(name.contains(" html ") || name.contains(" php ") || name.contains(" com ") || name.contains(" org ") || name.contains(".com")){
                    a.score = -50.0;
                }
            }
            /**
             * End of scoring algorithm
             */

            Collections.sort(attractions); // Sort the list of attractions by their score.

            // Print out the sorted list of candidates to the console.
            for(int i = 0; i<attractions.size(); i++){
                System.out.printf("%2d) %-35s %5.2f\n",
                    i+1, attractions.get(i).name, attractions.get(i).score);

                if(attractions.get(i).numReviews > 0 || attractions.get(i).certificate){
                    reviewsFound++;
                }
            }
            System.out.println("cat_count: " + person.cat_count);
            System.out.println("cat_occurance: " + person.cat_occurance);
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

        System.out.println("Number of attractions we got info for: "  + reviewsFound);
        // Close IO objects
        in.close();
        pw.close();
    }
}
