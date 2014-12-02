package imax.monitor.server;

import imax.monitor.client.MonitorService;
import imax.monitor.shared.IMonitor;
import imax.monitor.shared.MovieMonitor;
import imax.monitor.shared.SeatMonitor;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

@SuppressWarnings("serial")
public class MonitorServiceImpl extends RemoteServiceServlet implements
        MonitorService {

    private final static String SECRET_CODE = "47a9e8477d88a1d789562e9232fcee74";
    private final static String ALTER_CODE = "e41c0e2e5fa9832f3ba66013129b9096";

    private final static String TYPE_SEAT = "seat";
    private final static String TYPE_MOVIE = "movie";

    private static List<SeatMonitor> monitors;
    private static List<MovieMonitor> movieMonitors;
    protected static Object mutex = new Object();

    public String addMonitor(IMonitor mon, String code) {
        try {
            if (!isCorrectCode(code)) {
                return "Incorrect access code";
            }
            
            DatastoreService datastore = DatastoreServiceFactory
                    .getDatastoreService();
            Entity ent = new Entity("Monitors");
            if (mon instanceof SeatMonitor) {
                SeatMonitor m = (SeatMonitor)mon;
                ent.setProperty("ids", int2str(m.getIds()));
                ent.setProperty("rows", int2str(m.getRows()));
                ent.setProperty("seats", int2str(m.getSeats()));
                ent.setProperty("type", TYPE_SEAT);
                getMonitors().add(m);
            } else {
                MovieMonitor m = (MovieMonitor)mon;
                ent.setProperty("ids", m.getMovieId() + "");
                ent.setProperty("type", TYPE_MOVIE);
                ent.setProperty("date", m.getDate());
                getMovieMonitors().add(m);
            }
            ent.setProperty("email", mon.getEmail());
            datastore.put(ent);
            log("New monitor added:\n" + mon);
            return "Monitor has been added successfully";
        } catch (Exception e) {
            log("Error while processing", e);
            return "Error while processing:\n" + e.getMessage();
        }
    }
    
    protected synchronized static List<SeatMonitor> getMonitors() {
        if (monitors == null) {
            synchronized (mutex) {
                if (monitors == null)
                    initMonitors();
            }
        }
        return monitors;
    }
    
    protected synchronized static List<MovieMonitor> getMovieMonitors() {
        if (movieMonitors == null) {
            synchronized (mutex) {
                if (movieMonitors == null)
                    initMonitors();
            }
        }
        return movieMonitors;
    }
    
    protected static void initMonitors() {
        DatastoreService datastore = DatastoreServiceFactory
                .getDatastoreService();
        
        Query query = new Query("Monitors");
        
        List<Entity> monitorsEnt = datastore.prepare(query)
                .asList(FetchOptions.Builder.withDefaults());

        monitors = new ArrayList<SeatMonitor>();
        movieMonitors = new ArrayList<MovieMonitor>();
        for (Entity ent : monitorsEnt) {
            if (!TYPE_MOVIE.equals(ent.getProperty("type"))) {
                monitors.add(new SeatMonitor(str2int(ent.getProperty("ids").toString()), 
                        str2int(ent.getProperty("rows").toString()), 
                        str2int(ent.getProperty("seats").toString()), 
                        ent.getProperty("email").toString()));
            } else {
                movieMonitors.add(new MovieMonitor(ent.getProperty("ids").toString(), 
                        ent.getProperty("date").toString(),
                        ent.getProperty("email").toString()));
            }
        }
    }
    
    private static int[] str2int(String str) {
        String[] words = str.split(",");
        int[] ar = new int[words.length];
        for (int i = 0; i < words.length; i++)
            ar[i] = Integer.valueOf(words[i].trim());
        return ar;
    }
    
    private static String int2str(int[] ar) {
        String str = Arrays.toString(ar);
        return str.substring(1, str.length() - 1); // remove [ ]
    }
    
    private boolean isCorrectCode(String code) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        StringBuffer hexString = new StringBuffer();
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hash = md.digest(code.getBytes("UTF-8"));

        for (int i = 0; i < hash.length; i++) {
            if ((0xff & hash[i]) < 0x10) {
                hexString.append("0"
                        + Integer.toHexString((0xFF & hash[i])));
            } else {
                hexString.append(Integer.toHexString(0xFF & hash[i]));
            }
        }
        return SECRET_CODE.equals(hexString.toString()) || ALTER_CODE.equals(hexString.toString());
    }
}
