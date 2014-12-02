package imax.monitor.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.xpath.XPathExpressionException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


public final class AdapterAPI {
    
    private static final String URL = "https://cabinet.planeta-kino.com.ua/hall-scheme/?theater=imax-kiev&showtime={id}sector=&r-id={time}";
    private static final String MOVIE_URL = "http://planeta-kino.com.ua/ua/showtimes/xml/";
    
    private static final int FREE_STATUS = 1;
    
    public AdapterAPI() {

    }

    private String getUrl(int id, long time) {
        return URL.replace("{id}", "" + id).replace("{time}", "" + time); //$NON-NLS-1$
    }
    
    public List<Seat> getAvailableSeats(int id) throws IOException {
        long time = new Date().getTime();
        String json = requestHttp(getUrl(id, time));
        return extractSeats(json, id, time);
    }

    private List<Seat> extractSeats(String json, int id, long time) throws IOException {
        JsonObject root = null;
        try {
            root = new JsonParser().parse(json).getAsJsonObject();
        } catch (Exception e) {
            throw new IOException("Cannot parse json:\n"
                    + json.substring(0, Math.min(json.length(), 800)) + "...");
        }
        if (root.get("code").getAsInt() != 1) {
            throw new IllegalStateException("Wrong result status:" + root.get("message").getAsString());
        }
        try {
            JsonObject scheme = root.get("scheme").getAsJsonObject();
            JsonArray seats = scheme.get("seat").getAsJsonArray();
            List<Seat> list = new ArrayList<Seat>(seats.size() / 2);
            for (int i = 0; i < seats.size(); i++) {
                JsonObject o = seats.get(i).getAsJsonObject();
                if (o.get("status").getAsInt() == FREE_STATUS) {
                    list.add(new Seat(id, o.get("row").getAsInt(), o.get("seat").getAsInt(), time));
                }
            }
            return list;
        } catch (NullPointerException e) {
            throw new IOException("Cannot parse json:\n"
                    + json.substring(0, Math.min(json.length(), 800)) + "...");
        }
    }
    
    public Set<String> extractMovies(String name) throws IOException, XPathExpressionException {
        JSONObject json = XML.toJSONObject(requestHttp(MOVIE_URL));
        Set<String> result = new HashSet<String>();
        try {
            List<Integer> ids = new ArrayList<>();
            JSONArray movies = json.getJSONObject("planeta-kino").getJSONObject("movies").getJSONArray("movie");
            for (int i = 0; i < movies.length(); i++) {
                if (movies.getJSONObject(i).getString("url").contains(name))
                    ids.add(movies.getJSONObject(i).getInt("id"));
            }
            JSONArray showtimes = json.getJSONObject("planeta-kino").getJSONObject("showtimes").getJSONArray("day");
            for (int id : ids) {
                for (int i = 0; i < showtimes.length(); i++) {
                    JSONObject day = showtimes.getJSONObject(i);
                    JSONArray shows = day.getJSONArray("show");
                    for (int j = 0; j < shows.length(); j++) {
                        if (shows.getJSONObject(j).getInt("movie-id") == id) {
                            result.add(day.getString("date"));
                            break;
                        }
                    }
                }  
            }
        } catch (JSONException | NullPointerException e) {
            throw new IOException("Invalid json:\n" + json.toString(4), e);
        } 
        //System.out.println(jsonPrettyPrintString);
        //String result = xpath.evaluate("/planeta-kino/movies/movie[contains(@url, '" + name + "')]", xml);
        return result;
    }

    private String requestHttp(String urlInput) throws IOException {
        URL url;
        HttpURLConnection conn;
        BufferedReader rd;
        String line;
        String result = ""; //$NON-NLS-1$
        try {
            url = new URL(urlInput);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET"); //$NON-NLS-1$
            rd = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8")); //$NON-NLS-1$
            while ((line = rd.readLine()) != null) {
                result += line;
            }
            rd.close();
        } catch (Exception e) {
            throw new IOException(e);
        }
        return result;
    }

    // test
    public static void main(String[] args) throws IOException, XPathExpressionException {
        System.out.println(new AdapterAPI().extractMovies("hobbit")); //$NON-NLS-1$
    }
}
