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
    public static TreeMap<Integer, Profile> profiles = new TreeMap<Integer, Profile>(); // WHY DOES THIS NEED TO BE A HASHTABLE???????
    //     private static String line;
    //     private static volatile transient int count = 0;
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
        //         int numProfiles = 0;
        while((line = in.readLine()) != null){
            if(line.equals("")){
                break;
            }
            //             new Thread(new Runnable(){
            //                     public void run(){
            Profile profile = new Profile(line);
            profiles.put(profile.getUser_ID(), profile);
            //                         count++;
            //                     }
            //                 }).start();
            //             numProfiles++;
            // Potentially start creating attractions based on each person's candidates
            // i.e. create a new attraction for each of their candidates, but don't fill them
        }

        //         while(numProfiles != count){}

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
     * This method is the full program which orders 30 suggestions for each profile and outputs
     * them in the 'SienaFinalOutput.json' file.
     */
    public static void main(String args[]) throws IOException
    {
        PrintWriter pw = new PrintWriter("testOutput.json");

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

        File saveFile = null;
        try{
            saveFile = new File("output.dat");
        }catch(Exception ex){
            System.err.println("Could not find output.dat in main directory");
            return;
        }

        boolean serialized = saveFile.exists();

        if(serialized){
            try(FileInputStream s = new FileInputStream("output.dat")) {
                ObjectInputStream s2 = new ObjectInputStream(s);
                profiles = (TreeMap<Integer, Profile>)s2.readObject();
            }catch(Exception e){
                System.err.println("error finding output.dat");
            }
        }else{
            buildProfiles();
            try(FileOutputStream f = new FileOutputStream("output.dat")){
                ObjectOutputStream f2 = new ObjectOutputStream(f);
                f2.writeObject(profiles);
                f2.flush();
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        //cycles through the profiles in the hashmap
        Set<Integer> people = profiles.navigableKeySet();
        for(Integer num : people)
        {
            Profile person = profiles.get(num);
            ArrayList<Attraction> attractions = new ArrayList<Attraction>();
            attractions = person.candidates;

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

            Collections.sort(attractions);

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

        in.close();
        pw.close();
    }
}
