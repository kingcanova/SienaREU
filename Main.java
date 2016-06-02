import java.util.*;
import java.nio.*;
import java.nio.file.*;
import java.io.*;
/**
 * This class is the Main Program of the Contextual Suggestion TREC Track for Siena College. 
 * 
 * @author (your name) 
 * @version (a version number or a date)
 */
public class Main
{
    // This member is a map which maps profile IDs to their respective profile
    public static Hashtable<Integer, Profile> profiles = new Hashtable<Integer, Profile>(); // WHY DOES THIS NEED TO BE A HASHTABLE???????
    
    // This member is a map which maps attraction IDs to their respective attraction
    public static HashMap<Integer, Attraction> attrs = new HashMap<Integer, Attraction>();

    // Maps a context id with every attraction within that context
    public static HashMap<Integer, ArrayList<Attraction>> contextMap = new HashMap<Integer, ArrayList<Attraction>>();

    /**
     * This method builds each individual profile and puts them in 'profiles'. It should be
     * noted that each profile is taken from the 'batch_requests.json' file (from TREC).
     */
    private static void buildProfiles() throws IOException
    {
        BufferedReader in = null;
        try{
            in = new BufferedReader(new FileReader(Paths.get("batch_requests.json").toFile()));
        }catch(FileNotFoundException ex){
            System.err.println("Could not find batch_requests.json in main directory");
            return;
        }
        String line = "";
        while((line = in.readLine()) != null){
            if(line.equals("")){
                break;
            }
            Profile profile = new Profile(line);
            profiles.put(profile.getUser_ID(), profile);
            // Potentially start creating attractions based on each person's candidates
            // i.e. create a new attraction for each of their candidates, but don't fill them
        }

        Set<Integer> people = profiles.keySet();
        for(Integer num : people){
            Profile person = profiles.get(num);
            Set<String> keys = person.getCat_occurance().keySet();
            for(String cat: keys){
                person.getCat_count().put(cat, (person.getCat_count().get(cat)/person.getCat_occurance().get(cat)));
            }
        }
        in.close();
    }

    /**
     * This method builds each individual attraction and puts them in 'attractions'. 
     */
    private static void buildAttractions() throws IOException
    {
        File saveFile = null;
        try{
            saveFile = new File("saveFile.txt");
        }catch(Exception ex){
            System.err.println("Could not find saveFile.txt in main directory");
            return;
        }

        boolean serialized = saveFile.exists();
        if(serialized){ // Use previously created save file if one exists
            BufferedReader in = new BufferedReader(new FileReader(saveFile));
            String info = "";

            while((info = in.readLine()) != null){ // Read every attraction from the save file
                if(info.equals("")){
                    continue; // Skip each empty line
                }
                info += "\n";
                String line = "";
                while((line = in.readLine()) != null && !line.equals("")){ // Append each category
                    info += line + "\n";
                }
                Attraction attr = new Attraction(info);
                attrs.put(attr.id,attr);
            }
            in.close();
        }
    }

    private static void buildContextMap() throws IOException
    {
        BufferedReader br = new BufferedReader(new FileReader(
                    Paths.get("batchCollectionCategorizedWithId.txt").toFile()));
        String line = " ";
        String name = "";
        int attrID, conID;
        ArrayList<Attraction> temp = null;
        ArrayList<String> cats = new ArrayList<String>();
        while( (line = br.readLine()) != null )
        {
            name = line;
            attrID = Integer.parseInt(br.readLine());
            conID = Integer.parseInt(br.readLine());
            line = br.readLine();
            while( line != null && !line.equals(""))
            {
                cats.add(line);
                line = br.readLine();
            }            
            if (contextMap.get(conID) == null)
            {//If first spot is empty
                temp = new ArrayList<Attraction>();
                temp.add(new Attraction(name, cats, attrID));
                contextMap.put(conID, temp);
            }
            else
            {//Already contains an arraylist
                temp = contextMap.get(conID);
                temp.add(new Attraction(name, cats, attrID));
                contextMap.put(conID, temp);
            }
            cats = new ArrayList<String>();
        }
        br.close();
    }

    /**
     * This method is the full program which orders 30 suggestions for each profile and outputs
     * them in the 'SienaFinalOutput.json' file.
     */
    public static void main(String args[]) throws IOException
    {
        buildAttractions();
        buildProfiles();
        buildContextMap();

        PrintWriter pw = new PrintWriter("testOutput.json");
        //reset scores in hashtable since it's static and are saved unless JVM is reset

        //fill up array with categories we want to ignore for scoring purposes i.e. establishment, point of interest, etc
        Scanner in = null;
        try{
            in = new Scanner(new File("UneccessaryCats.txt"));
        }catch(FileNotFoundException ex){
            System.err.println("UneccessaryCats.txt was not found in the main directory");
            return;
        }
        ArrayList<String> ignoredCats = new ArrayList<String>();

        while (in.hasNextLine()){
            ignoredCats.add(in.nextLine());
        }

        //cycles through the profiles in the hashmap
        //Set<Integer> people = profiles.navigableKeySet();
        Set<Integer> people = profiles.keySet();
        HashMap<Integer, ArrayList<Attraction>> testMap = new HashMap<Integer, ArrayList<Attraction>>();
        for(Integer num : people)
        {
            Profile person = profiles.get(num);
            ArrayList<Attraction> attractions = new ArrayList<Attraction>();

            for (Attraction a : contextMap.get(person.getContextID())) 
            {
                if (person.getAttractions().contains(a.id))
                    attractions.add(a);
            }

            for (Attraction a : attractions)
            {
                boolean hasCategories = false; 
                for(String cat : a.categories)
                {
                    hasCategories = true;
                    if(person.getCat_count().get(cat) != null && !ignoredCats.contains(cat))
                    {
                        a.score += person.getCat_count().get(cat);
                        a.count += 1;
                    }
                }

                if(a.count > 0){
                    a.score = a.score / a.count;
                }
                //place the attractions that don't have categories at the very end of the suggested list
                else if(!hasCategories)
                    a.score = -Double.MAX_VALUE;
            }

            //sort attractions before the penalty function
            Collections.sort(attractions);

            testMap.put(num, attractions);
            //print out the attrations and their scores before the penalty function
            for(int i = 0; i<attractions.size(); i++){
                System.out.printf("%2d) %-35s %5.2f\n",
                    i+1, attractions.get(i).name, attractions.get(i).score);
            }

            System.out.println("Sorted Results:     " + person.getUser_ID());

            pw.print("{\"body\": {\"suggestions\": [");

            int size = attractions.size();

            for (int k=0; k<size; k++){
                int currID = attractions.get(k).id;
                String id = "00000000" + currID;
                id = id.substring(id.length()-8);
                pw.print("\"TRECCS-" + id + "-" + person.getContextID() + "\"");
                if(k != size - 1) {pw.print(",");}

            }
            pw.print("]}, \"groupid\": \"Siena_SUCCESS\", \"id\": " + num +
                ", \"runid\": \"SCIAIrunA\"}");
            pw.println();
        }

        //         for(Integer num : people){
        //             Profile person = profiles.get(num);
        //             ArrayList<Attraction> attractions = testMap.get(num);
        //             Collections.sort(attractions);
        //             
        //             for(int i = 0; i<attractions.size(); i++){
        //                 System.out.printf("%2d) %-35s %5.2f\n",
        //                     i+1, attractions.get(i).name, attractions.get(i).score);
        //             }
        // 
        //             System.out.println("Sorted Results:     " + person.getUser_ID());
        //             
        //             pw.print("{\"body\": {\"suggestions\": [");
        // 
        //             int size = attractions.size();
        // 
        //             for (int k=0; k<size; k++){
        //                 int currID = attractions.get(k).id;
        //                 String id = "00000000" + currID;
        //                 id = id.substring(id.length()-8);
        //                 pw.print("\"TRECCS-" + id + "-" + person.getContextID() + "\"");
        //                 if(k != size - 1) {pw.print(",");}
        // 
        //             }
        //             pw.print("]}, \"groupid\": \"Siena_SUCCESS\", \"id\": " + num +
        //                 ", \"runid\": \"SCIAIrunA\"}");
        //             pw.println();
        //         }
        in.close();
        pw.close();
    }
}
