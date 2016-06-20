import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import javax.json.JsonReader;
import javax.json.Json;
import java.nio.charset.Charset;
import java.nio.file.*;
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
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.methods.*;
import org.apache.http.util.*;
import org.apache.http.params.*;
import javax.swing.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.HttpHost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.HttpEntity;
/**
 * An Attraction contains all the information on a specific point of interest.
 * 
 * @author Tristan Canova, Dan Carpenter, Kevin Danaher, Neil Devine
 * @version 6/9/16
 */
public class Attraction implements Comparable<Attraction>, Serializable
{
    public static transient List<Prox> proxies = new ArrayList<>();
    public static transient int numAttractions = 0;

    static{ // Read in from list of proxies and add each proxy to 'proxies'
        try{
            BufferedReader in = new BufferedReader(new FileReader(Paths.get("../DataFiles/proxylist.csv").toFile()));
            String line = "";
            while((line = in.readLine()) != null){
                String[] row = line.split(",");
                //System.out.println(Arrays.toString(row));
                Prox p = new Prox(row[0], row[1]);
                proxies.add(p);
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    public String name;
    public int contextID;
    public int id;
    public double rating;
    public ArrayList<String> categories;

    public Double score = 0.0;
    public int count;

    // Specifically for TripAdvisor:
    public boolean certificate; // Whether this attraction has a 'Certificate of Excellence'
    public int numReviews; // The number of people who reviewed this attraction
    public String travelerTypes; // A List of the most relevant traveler types on TripAdvisor
    public String seasons; // A List of the most relevant seasons to go to this attraction
    /**
     * Plain Data Structure for storing relevant information for a given attraction based on its API search results
     */
    class WebInfo 
    {
        // For most APIs:
        private double apiRating;
        private ArrayList<String> apiCategories;

        // Constructor for most APIs
        private WebInfo(double r, ArrayList<String> c) 
        {
            apiRating = r;
            apiCategories = c;
        }
    }

    /**
     * Attraction Constructor that is used when no file exists, and you need
     * to query the APIs to compile information about the attraction.
     *
     * @param title The name of the attraction
     * @param coords The location of the attraction
     */
    public Attraction(String title, Context context) 
    {
        WebInfo fs, yp, gp, ta;
        fs = yp = gp = ta = null;

        String coords = context.coordinates;
        String location = context.location;
        try 
        {
            fs = searchFourSquare(coords, title);
        }
        catch (URISyntaxException | IOException e) 
        {
            System.err.println("Just FourSquare...");
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

        try
        {
            ta = searchTripAdvisor(title, context.geoID);
        }
        catch (URISyntaxException | IOException | ParseException e) 
        {
            e.printStackTrace();
        }
        name = title;
        double numRatings = 0;
        this.rating = 0;
        ArrayList<String> combinedCategories = new ArrayList<String>();
        //a rating of 0 is an indication that no real rating was found
        if (fs != null) {
            if (fs.apiRating != 0) {
                rating = fs.apiRating*2;
                numRatings += 2;
            }
            for (String cat: fs.apiCategories) {
                combinedCategories.add(cat);
            }
        }
        if (yp != null) {
            if (yp.apiRating != 0) {
                rating += yp.apiRating;
                numRatings += 1;

            }
            for (String cat: yp.apiCategories) {
                if (!combinedCategories.contains(cat)) 
                    combinedCategories.add(cat);
            }
        }
        if (gp != null){
            if (gp.apiRating != 0) {
                rating += gp.apiRating;
                numRatings += 1;
            }
            for (String cat: gp.apiCategories) {
                if (!combinedCategories.contains(cat)) 
                    combinedCategories.add(cat);
            }
        }
        if (ta != null){
            if (ta.apiRating != 0) {
                rating += ta.apiRating;
                numRatings += 1;
            }
            for (String cat: ta.apiCategories) {
                if (!combinedCategories.contains(cat)) 
                    combinedCategories.add(cat);
            }
        }
        if (numRatings != 0)
            this.rating /= numRatings;
        else this.rating = 0;
        this.categories = combinedCategories;
    }

    /**
     * Copy constructor for Attraction Objects
     * @param attr The attraction to be copied
     */
    public Attraction(Attraction attr){
        this.name = attr.name;
        this.contextID = attr.contextID;
        this.id = attr.id;
        this.rating = attr.rating;
        this.categories = attr.categories;

        this.score = attr.score;
        this.count = attr.count;
        this.certificate = attr.certificate; 
        this.numReviews = attr.numReviews; 
        this.travelerTypes = attr.travelerTypes; 
        this.seasons = attr.seasons;
    }

    /**
     * Query the FourSquare API 
     * 
     * @param coords a string of the latitude and longitude of the context
     * @param name a string of the attraction to search
     * @return a WebInfo object of the search results
     */
    public WebInfo searchFourSquare(String coords, String name) 
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
        //HttpClient client = HttpClientBuilder.create().build();
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(20 * 1000).build();
        HttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
        final HttpResponse execute = client.execute(request);
        final String r = EntityUtils.toString(execute.getEntity());
        //client.getConnectionManager().shutdown();

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
        JSONObject curr;
        try{
            if (results.size() == 0)
            {
                //No results found, return a blank suggestion
                return null;
            }
            //System.out.println(results);
            curr = (JSONObject) results.get(0);
        }catch(Exception ex){
            return null;
        }

        String heading = (curr.get("name")).toString().split(" - ")[0].split(" \\| ")[0];

        String a, b;
        if(heading.length() > name.length()){
            b = heading;
            a = name;
        }else{
            a = heading;
            b = name;
        }
        a = a.replace("+", " ");
        b = b.replace("+"," ");
        double similarity = Lev.similarity(a,b);

        System.out.println("FS: Shorter name:  " + a);
        System.out.println("FS: Longer name:  " + b);
        System.out.println("FS: Similarity:  " + similarity);

        if(similarity > 30){
            return null;
        }

        //new code to try and scrape rating from FourSquare page
        //works sometimes, but other times the menu field doesnt even exist
        JSONObject menu = (JSONObject) curr.get("menu");
        String rating = "";
        double adjustedRating = 0.0;
        if (menu != null) {
            String menuUrl = menu.get("url").toString();
            menuUrl = menuUrl.substring(0, menuUrl.length()-4);
            Document doc = Jsoup.connect(menuUrl).get();
            Elements ratingSpan = doc.select("span.venueScore");
            boolean first = true;
            for (Element e : ratingSpan) {
                if (first) {
                    rating = e.text().substring(0, e.text().length()-3);
                }
                first = false;
            }
            try{
                adjustedRating = Double.parseDouble(rating);
            }catch(NumberFormatException ex){
                adjustedRating = 0;
            }
            //mapping FourSquare ratings to a 1-5 scale as follows:
            //1&2 -> 1, 3&4 -> 2, 5&6 -> 3, 7&8 -> 4, 9&10 -> 5
            adjustedRating = Math.ceil(adjustedRating/2.0);
        }
        else {
            adjustedRating = 0.0;
        }

        JSONArray cats = ((JSONArray)(curr.get("categories")));
        String id = curr.get("id").toString();

        String[] types = new String[cats.size()];
        for(int x = 0; x < cats.size(); x++)
        {
            types[x] = ((JSONObject)cats.get(x)).get("shortName").toString().toLowerCase().replace(" ","");
        }
        return new WebInfo(adjustedRating, new ArrayList<String>(Arrays.asList(types))); 
    }

    /**
     * Query the YellowPages API
     *
     * @param name The name of the attraction
     * @param lat Latitude of the attraction
     * @param lng Longitude of the attraction
     * @return a WebInfo object of the search results
     */
    public WebInfo searchYellowPages(String name, double lat, double lng)
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
            System.err.println("Error: null pointer in YPqAPI query:\n" + e);
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
        JSONArray results = new JSONArray();
        if(cur == null){
            return null;
        }
        if (cur.get("searchListings") != null) {
            if (!(cur.get("searchListings") instanceof String)) {
                JSONObject listings = (JSONObject) cur.get("searchListings");
                results = (JSONArray) (listings.get("searchListing"));
            }
        }
        else {
            results = (JSONArray) cur.get("searchListing");
        }


        String rating = "";
        ArrayList<String> types = new ArrayList<String>();
        //Check if Yellow Pages returned any results
        try {
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
                for (String cat : cats) {
                    for (String inner : cat.split("\\|")) {
                        types.add(inner.toLowerCase());
                    }
                }

                String heading = unk.get("businessName").toString().split(" - ")[0].split(" \\| ")[0];

                String a, b;
                if(heading.length() > name.length()){
                    b = heading;
                    a = name;
                }else{
                    a = heading;
                    b = name;
                }
                a = a.replace("+", " ");
                b = b.replace("+"," ");
                double similarity = Lev.similarity(a,b);

                System.out.println("YP: Shorter name:  " + a);
                System.out.println("YP: Longer name:  " + b);
                System.out.println("YP: Similarity:  " + similarity);

                if(similarity > 30){
                    return null;
                }
            }
            return new WebInfo(Double.parseDouble(rating), types);
        } catch (NullPointerException e) {
            return null;
        }
    }

    /**
     * Query the GooglePlaces API 
     * @param name a string of the attraction to search
     * @param lat a double of the latitude of the context
     * @param lon a double of the longitude of the context
     * @return a WebInfo object of the search results
     */
    public WebInfo searchGooglePlaces(String name, double lat, double lng) 
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
            System.err.println("Error: null pointer in GPqAPI query:\n" + e);
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
        ArrayList<String> types = new ArrayList<String>();
        //Check if Google Places returned any results
        try {
            if(results.size() == 0)
            {
                //No results found, return a blank suggestion
                return null;
            }
            else
            {
                JSONObject unk = (JSONObject) results.get(0);
                rating = unk.get("rating").toString();
                //parse the JSON formatted list of categories
                JSONArray stuff = (JSONArray) parser.parse(unk.get("types").toString());
                for (int i = 0; i < stuff.size(); i++) {
                    types.add(stuff.get(i).toString());
                }

                String heading = unk.get("name").toString().split(" - ")[0].split(" \\| ")[0];

                String a, b;
                if(heading.length() > name.length()){
                    b = heading;
                    a = name;
                }else{
                    a = heading;
                    b = name;
                }
                a = a.replace("+", " ");
                b = b.replace("+"," ");
                double similarity = Lev.similarity(a,b);

                System.out.println("GP: Shorter name:  " + a);
                System.out.println("GP: Longer name:  " + b);
                System.out.println("GP: Similarity:  " + similarity);

                if(similarity > 30){
                    return null;
                }

            }
            return new WebInfo(Double.parseDouble(rating), types);
        }
        catch (NullPointerException e) {
            return null;
        }
    }

    /**
     * Scrape the TripAdvisor Website for relevant information on the attraction
     * @param name A string of the attraction to search
     * @param geoID A string containing the TripAdvisor geo ID of the city of this attraction
     * @return a WebInfo object of the search results
     */
    public WebInfo searchTripAdvisor(String name, String geoID)
    throws ParseException, IOException, URISyntaxException
    {
        String trip = "https://www.tripadvisor.com";
        name = name.replaceAll("\"", "").trim().replaceAll(" ", "+");

        String searchUrl = trip + "/Search" + "?q=" + name + "&geo=" + geoID;
        Document doc = null;
        while(true){
            Prox p = proxies.get(45);
            //System.out.println(ensocketize(p));
            System.setProperty("https.proxyHost", p.ip);
            System.setProperty("https.proxyPort", p.port);
            if(++numAttractions % 30 == 0){
                System.out.println("................Switching proxy...................");
                Collections.rotate(proxies, 1);
                //while(!ensocketize(proxies.get(0))){
                //    Collections.rotate(proxies,1);
                //}
            }

            //Document doc = Jsoup.connect(searchUrl).get();
            try{
                doc = Jsoup.connect(searchUrl).get();
                break;
            }catch(Exception e){
                Collections.rotate(proxies,1);
            }
        }
        //userAgent("Mozilla/5.0")
        Elements links = doc.select("#taplc_search_results_0 .body div");
        if(links.size() > 0){
            Element link = links.get(0);
            String onclick = link.attr("onclick");
            int index = onclick.indexOf(".loadURLIfNotLink(event,'");
            if(index == -1){
                return null;
            }
            String almostURL = onclick.substring(onclick.indexOf(".loadURLIfNotLink(event,'")+25);
            String url = trip + almostURL.substring(0, almostURL.indexOf(".html") + 5);

            Document deezNutz = Jsoup.connect(url).userAgent("Mozilla/5.0").get();

            String heading = deezNutz.select("#HEADING").text().split(" - ")[0].split(" \\| ")[0];
            String a, b;
            if(heading.length() > name.length()){
                b = heading;
                a = name;
            }else{
                a = heading;
                b = name;
            }
            a = a.replace("+", " ");
            b = b.replace("+"," ");
            double similarity = Lev.similarity(a,b);

            System.out.println("Shorter name:  " + a);
            System.out.println("Longer name:  " + b);
            System.out.println("Similarity:  " + similarity);

            if(similarity > 25){
                return null;
            }

            boolean excellent = deezNutz.select("div.coeBadgeDiv span.taLnk").size() > 0;
            ArrayList<String> tags = new ArrayList<String>();
            Elements reviewTags = deezNutz.select("div.ui_tagcloud_group span.ui_tagcloud");
            for(int i = 1; i < reviewTags.size(); i++){
                //tags.add(reviewTags.get(i).text()); // Discrediting TripAdvisor User Tags
            }

            //Elements legitCats = deezNutz.select("div.detail");

            String rev = deezNutz.select("div.rating a.more").text().replace(" Reviews", "").replace(",","").replace(" Review", "");
            int numReviews = 0;
            if(!rev.equals("")){
                numReviews = Integer.parseInt(rev);
            }

            String rate = deezNutz.select("span.sprite-rating_rr img.sprite-rating_rr_fill").attr("content");
            double overallRating = 0.0;
            if(!rate.equals("")){
                overallRating = Double.parseDouble(rate);
            }

            String types = deezNutz.select("div.col.segment.extrawidth ul li label span").toString();

            String seas = deezNutz.select("div.col.season.extrawidth ul li label span").toString();

            //WebInfo info = new WebInfo(overallRating, tags, excellent, numReviews, types, seas);
            WebInfo info = new WebInfo(overallRating, tags);
            certificate = excellent;
            this.numReviews = numReviews;
            travelerTypes = types;
            seasons = seas;

            return info;

            //System.out.println(url);
            //System.out.println(seas);
        }

        return null;
    }

    public int compareTo(Attraction other)
    {
        if(this.score < other.score)
        {
            return 1;
        }
        else if (this.score > other.score)
        {
            return -1;
        }
        return 0;
    }

    public String toString() {
        String out = name + "\n";
        out += id + "\n";
        out += rating +"\n";
        out += certificate + "\n";
        out += numReviews + "\n";
        //out += travelerTypes + "\n";
        //out += seasons + "\n";
        out += categories +"\n";
        return out;
    }

    public static void test() {
        Scanner in = new Scanner(System.in);
        System.out.print("Attraction: ");
        String name = in.nextLine();
        System.out.print("Coordinates: ");
        String coords = in.nextLine();
        System.out.print("TripAdvisor geo ID (Optional): ");
        String geo = in.nextLine();
        Attraction a = new Attraction(name, new Context("", coords, geo));
        System.out.println(a);
    }
}
