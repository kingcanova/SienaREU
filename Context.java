/**
 * Plain Data Structure which merely stores the city and state (location), latitude and longitude (location).
 */
public class Context
{
    String location;
    String coordinates;
    String geoID; // Specifically for TripAdvisor

    public Context(String loc, String coord, String geo){
        location = loc;
        coordinates = coord;
        geoID = geo;
    }
}