import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import javax.json.JsonReader;
import javax.json.Json;
import java.nio.charset.Charset;
import fi.foyt.foursquare.api.FoursquareApi;
import fi.foyt.foursquare.api.FoursquareApiException;
import fi.foyt.foursquare.api.Result;
import fi.foyt.foursquare.api.entities.CompactVenue;
import fi.foyt.foursquare.api.entities.VenuesSearchResult;
import java.io.*;
import java.net.*;
import java.util.*;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.methods.*;
import org.apache.http.util.*;
import javax.swing.*;
/**
 * An Attraction contains all the information on a specific point of interest.
 * 
 * @author Tristan Canova, Dan Carpenter, Kevin Danaher, Neil Devine
 * @version 0.1
 */
public class Attraction
{
    private String name;
    //private double lat;
    //private double lng;
    //private String city;
    private int contextID;
    private int id;
    private double rating;
    private ArrayList<String> categories;

    class RatingAndCategories 
    {
        private double rating;
        private ArrayList<String> categories;

        private RatingAndCategories(double r, ArrayList<String> c) 
        {
            rating = r;
            categories = c;
        }
    }

    /**
     * Attraction Constructor that is used when a file exists, so you just read
     * through the file to get the information needed on the attraction.
     * 
     * Pre-condition - Information should be passed in this form:
     *      Attraction Name
     *      ID
     *      Rating
     *      Category 1
     *      .
     *      .
     *      Category n
     *
     * @param info All of the information that we have on this attraction
     */
    public Attraction(String info) 
    {
        String[] infoSplit = info.split("\n");
        name = infoSplit[0];
        id = Integer.parseInt(infoSplit[1]);
        rating = Double.parseDouble(infoSplit[2]);
        for (int i = 3; i < infoSplit.length; i++) 
        {
            categories.add(infoSplit[i]);
        }
    }

    /**
     * Attraction Constructor that is used when no file exists, and you need
     * to query the APIs to compile information about the attraction.
     *
     * @param title The name of the attraction
     * @param coords The location of the attraction
     */
    public Attraction(String title, String coords) 
    {
        RatingAndCategories fs, yp, gp;
        fs = yp = gp = null;
        try 
        {
            fs = searchFourSquare(coords, title);
        }
        catch (URISyntaxException | IOException e) 
        {
            e.printStackTrace();
        }
        try 
        {
            int mid = coords.indexOf(',');
            yp = searchYellowPages(title, 
                Double.parseDouble(coords.substring(0, mid)), 
                Double.parseDouble(coords.substring(mid+1, coords.length())));
        }
        catch (URISyntaxException | IOException | ParseException e) 
        {
            e.printStackTrace();
        }
        try 
        {
            int mid = coords.indexOf(',');
            gp = searchGooglePlaces(title, 
                Double.parseDouble(coords.substring(0, mid)), 
                Double.parseDouble(coords.substring(mid+1, coords.length())));
        }
        catch (URISyntaxException | IOException | ParseException e) 
        {
            e.printStackTrace();
        }
        name = title;
        rating = fs.rating;
        categories = fs.categories;
    }

    /**
     * Query the FourSquare API 
     * 
     * @param coords a string of the latitude and longitude of the context
     * @param name a string of the attraction to search
     * @return a RatingAndCategories object of the search results
     */
    public RatingAndCategories searchFourSquare(String coords, String name) 
    throws URISyntaxException, IOException
    {
        String client_id = Secret.FOURSQUARE_CLIENT_ID; 
        String client_secret = Secret.FOURSQUARE_CLIENT_SECRET;
        String version = "20120609";

        final URIBuilder builder = new URIBuilder()
            .setScheme("https")
            .setHost("api.foursquare.com")
            .setPath("/v2/venues/search");

        //necessary paramaters to add for a FourSquareAPI search       
        builder.addParameter("client_id", client_id);
        builder.addParameter("client_secret", client_secret);
        builder.addParameter("v", version);
        builder.addParameter("ll", coords);//latitude and longitude
        builder.addParameter("query", name);

        //conduct search with above paramters and recieve String response
        final HttpUriRequest request = new HttpGet(builder.build());
        HttpClient client = HttpClientBuilder.create().build();
        final HttpResponse execute = client.execute(request);
        final String r = EntityUtils.toString(execute.getEntity());

        //turn String into JSON
        JSONParser parser = new JSONParser();
        JSONObject response = null;

        try {
            response = (JSONObject) parser.parse(r);
        } 
        catch (ParseException pe) {
            System.err.println("Error: could not parse JSON response:");
            System.out.println(r);
            System.exit(1);
        } 
        catch (NullPointerException e) {
            System.err.println("Error: null pointer in FSqAPI query:\n" + e);
            //
            //should this return a blank Suggestion too?
            //
            return null;
        }

        //array of terms used by FourSquareAPI inside of the JSON response to separate data
        String[] fqTerms = new String[]{"name", "location", "id", "contact", "rating", "categories"};

        //retrieves JSON data for all businesses found
        JSONObject venues = (JSONObject) response.get("response");
        JSONArray results = (JSONArray) venues.get("venues");

        //Check if FourSquare returned any results
        if (results.size() == 0)
        {
            //No results found, return a blank suggestion
            return null;
        }
        System.out.println(results);
        JSONObject curr = (JSONObject) results.get(0);
        JSONObject menu = (JSONObject) curr.get("menu");
        String menuUrl = menu.get("url").toString();
        menuUrl = menuUrl.substring(0, menuUrl.length()-4);
        String rating = "";
        if(curr.get("rating") == null)
        {
            rating = "0";
        }
        else
        {
            rating = (curr.get("rating")).toString();
        }

        JSONArray cats = ((JSONArray)(curr.get("categories")));
        String id = curr.get("id").toString();
        
        //now that we have the id of the place, we can query FourSquare
        //with that specific id and get the rating
        

        
        String[] types = new String[cats.size()];
        for(int x = 0; x < cats.size(); x++)
        {
            types[x] = ((JSONObject)cats.get(x)).get("shortName").toString().toLowerCase().replace(" ","");
        }
        return new RatingAndCategories(Double.parseDouble(rating), new ArrayList<String>(Arrays.asList(types))); 
    }

    /**
     * Query the YellowPages API
     *
     * @param name The name of the attraction
     * @param lat Latitude of the attraction
     * @param lng Longitude of the attraction
     * @return a RatingAndCategories object of the search results
     */
    public RatingAndCategories searchYellowPages(String name, double lat, double lng)
    throws ParseException, IOException, URISyntaxException
    {
        String YP_KEY  = Secret.YP_KEY;
        HttpClient client = HttpClientBuilder.create().build();

        final URIBuilder builder = new URIBuilder()
            .setScheme("http")
            .setHost("api2.yp.com")
            .setPath("/listings/v1/search");

        //necessary paramaters to add for a YellowPagesAPI search. Max search radius is 50 miles
        builder.addParameter("searchloc", lat + "," + lng);
        builder.addParameter("radius", "50"); //radius in miles
        builder.addParameter("term", name);
        builder.addParameter("key", YP_KEY);
        builder.addParameter("format", "json");

        //conduct search with above paramters and recieve String response
        final HttpUriRequest request = new HttpGet(builder.build());
        final HttpResponse execute = client.execute(request);
        final String r = EntityUtils.toString(execute.getEntity());

        //turn String into JSON
        JSONParser parser = new JSONParser();
        JSONObject response = null;
        try {
            response = (JSONObject) parser.parse(r);
        } 
        catch (ParseException pe) {
            System.err.println("Error: could not parse JSON response:");
            System.out.println(r);
            System.exit(1);
        }
        catch (NullPointerException e) {
            System.err.println("Error: null pointer in FSqAPI query:\n" + e);
            //
            //should this return a blank Suggestion too?
            //
            return null;
        }

        //array of terms used by YellowPagesAPI inside of the JSON response to separate data
        String[] ypTerms = new String[]{ "businessName", "averageRating", "latitude", 
                "longitude", "categories" };

        //retrieves JSON data for all businesses found
        JSONObject cur = (JSONObject) response.get("searchResult");
        cur = (JSONObject) cur.get("searchListings");
        JSONArray results = (JSONArray) (cur.get("searchListing"));

        String rating = "";
        ArrayList<String> types = null;
        //Check if Yellow Pages returned any results
        if(results.size() == 0)
        {
            //No results found, return a blank suggestion
            return  null;
        }
        else
        {
            JSONObject unk = (JSONObject) results.get(0);
            rating = unk.get("averageRating").toString();
            String[] cats = unk.get("categories").toString().split(",");
            types = new ArrayList<String>(Arrays.asList(cats));
        }
        return new RatingAndCategories(Double.parseDouble(rating), types);
    }

    /**
     * Query the GooglePlaces API 
     * @param name a string of the attraction to search
     * @param lat a double of the latitude of the context
     * @param lon a double of the longitude of the context
     * @return a suggestion object of the search results
     */
    public RatingAndCategories searchGooglePlaces(String name, double lat, double lng) 
    throws ParseException, IOException, URISyntaxException
    {
        String GOOGLE_API_KEY  = Secret.GOOGLE_API_KEY;
        HttpClient client = HttpClientBuilder.create().build();

        final URIBuilder builder = new URIBuilder()
            .setScheme("https")
            .setHost("maps." + "googleapis.com")
            .setPath("/maps/api/place/nearbysearch/json");

        //necessary paramaters to add for a GooglePlacesAPI search. Max for radius is 50,000 meters
        builder.addParameter("location", lat + "," + lng);
        builder.addParameter("radius", "15000");//radius in meters
        builder.addParameter("name", name);
        builder.addParameter("key", GOOGLE_API_KEY);

        //conduct search with above paramters and recieve String response
        final HttpUriRequest request = new HttpGet(builder.build());
        final HttpResponse execute = client.execute(request);
        final String r = EntityUtils.toString(execute.getEntity());

        //turn String into JSON
        JSONParser parser = new JSONParser();
        JSONObject response = null;
        try {
            response = (JSONObject) parser.parse(r);
        } catch (ParseException pe) {
            System.err.println("Error: could not parse JSON response:");
            System.out.println(r);
            System.exit(1);
        }
        catch (NullPointerException e) {
            System.err.println("Error: null pointer in FSqAPI query:\n" + e);
            //
            //should this return a blank Suggestion too?
            //
            return null;
        }

        //array of terms used by GooglePlacesAPI inside of the JSON response to separate data
        String[] googleTerms = new String[]{"name", "rating", "types",
                "vicinity", "id", "place_id", "geometry", };

        //retrieves JSON data for all businesses found
        JSONArray results = (JSONArray) response.get("results");

        String rating = "";
        ArrayList<String> types = null;
        //Check if Google Places returned any results
        if(results.size() == 0)
        {
            //No results found, return a blank suggestion
            return null;
        }
        else
        {
            JSONObject unk = (JSONObject) results.get(0);
            rating = unk.get("rating").toString();
            String[] cats = unk.get("types").toString().split(",");
            types = new ArrayList<String>(Arrays.asList(cats));
        }
        return new RatingAndCategories(Double.parseDouble(rating), types);
    }
    
    public String toString() {
        String out = name + "\n";
        out += rating +"\n";
        out += categories +"\n";
        return out;
    }

    public static void test() {
        Scanner in = new Scanner(System.in);
        System.out.print("Attraction: ");
        String name = in.nextLine();
        System.out.print("Coordinates: ");
        String coords = in.nextLine();
        Attraction a = new Attraction(name, coords);
        System.out.println(a);
    }
}
