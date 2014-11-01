package imax.monitor.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
    
    public String extractMovies() throws IOException {
        return requestHttp(MOVIE_URL);
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
    public static void main(String[] args) throws IOException {
        System.out.println(new AdapterAPI().getAvailableSeats(19414)); //$NON-NLS-1$
    }
}
