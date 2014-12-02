package imax.monitor.server;

import imax.monitor.shared.MovieMonitor;
import imax.monitor.shared.SeatMonitor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.xpath.XPathExpressionException;


public class SchedulerServiceImpl extends MonitorServiceImpl {

	private static final long serialVersionUID = -1290583620502335408L;
	
	private static final String URL = "https://cabinet.planeta-kino.com.ua/hall/?show_id={id}&theatre_id=imax-kiev&lang=ua";

    private static final AdapterAPI api = new AdapterAPI();
    private static final Mailer mailer = new Mailer();
    private static final Random rand = new Random();
    
    private static final int CACHE_CLEAR = 2; // 2 hours;

    private static HashSet<Seat> alarmed = new HashSet<>();
    private static long lastCheck;
    private static HashSet<MovieMonitor> movieCache = new HashSet<>();
	
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
	    
	    String type = request.getParameter("refresh");
	    if (type != null && !type.isEmpty()) {
	        log("Refreshing monitors");
	        synchronized (mutex) {
                initMonitors();
            }
	    }
		log("Starting checks");
        checkSeatMonitors();
        checkMovieMonitors();
		
	}
	
	private void checkSeatMonitors() {
	    log("Checking seats");
	    List<SeatMonitor> monitors = getMonitors();
        HashMap<Integer, Map<String, List<SeatMonitor>>> ids = new HashMap<>();
        HashMap<String, Boolean> emails = new HashMap<String, Boolean>();
        for (SeatMonitor m : monitors) {
            emails.put(m.getEmail(), false);
            for (int i = 0; i < m.getIds().length; i++) {
                if (!ids.containsKey(m.getIds()[i])) {
                    ids.put(m.getIds()[i], new HashMap<String, List<SeatMonitor>>());
                }
                Map<String, List<SeatMonitor>> map = ids.get(m.getIds()[i]);
                if (!map.containsKey(m.getEmail())) {
                    map.put(m.getEmail(), new ArrayList<SeatMonitor>());
                }
                map.get(m.getEmail()).add(m);
            }
        }
        
        checkCache();
        
        log("Got " + ids.size() + " ids, " + monitors.size() + " monitors and " + emails.size() + " users.");
        
        Map<String, Map<Integer, List<Seat>>> available = new HashMap<>();
        
        for (Entry<Integer, Map<String, List<SeatMonitor>>> entry : ids.entrySet()) {
            List<Seat> seats = null;
            try {
                seats = api.getAvailableSeats(entry.getKey());
            } catch (IllegalStateException e) {
                // todo remove from checks
                log("Id " + entry.getKey() + " is not working");
                continue;
            } catch (IOException e) {
                log("Error while fetching data for " + entry.getKey(), e);
                continue;
            }
            Map<String, List<SeatMonitor>> map = entry.getValue();
            for (String email : map.keySet()) {
                for (SeatMonitor m : map.get(email)) {
                    for (Seat s : seats) {
                        if (m.isRequired(s.row, s.seat)) {
                            if (!available.containsKey(email)) {
                                available.put(email, new HashMap<Integer, List<Seat>>());
                            } 
                            Map<Integer, List<Seat>> availForUser = available.get(email);
                            if (!availForUser.containsKey(entry.getKey())) {
                                availForUser.put(entry.getKey(), new ArrayList<Seat>());
                            }
                            availForUser.get(entry.getKey()).add(s);
                            if (!alarmed.contains(s)) {
                                emails.put(email, true);
                                synchronized (alarmed) {
                                    alarmed.add(s);
                                }
                            }
                        }
                    }
                }
            }
            
            try {
                Thread.sleep(rand.nextInt(2000)); // delay
            } catch (InterruptedException e) {
                e.printStackTrace();
            } 
        }
        
        for (String email : emails.keySet()) {
            if (emails.get(email)) {
                log("Sending email to " + email);
                String message = generateMessage(available.get(email));
                try {
                    mailer.sendMail(email, email, "New seats are available", message);
                } catch (Exception e) {
                    log("Error while sending email to " + email, e);
                }
            }
            
        }
	}
	
	private void checkMovieMonitors() {
	    log("Checking movie monitors");
	    List<MovieMonitor> monitors = getMovieMonitors();
	    log("Got " + monitors.size() + " monitors");
	    Map<String, Set<String>> dates = new HashMap<>();
	    try {
	        for (MovieMonitor m : monitors) {
	            if (!dates.containsKey(m.getMovieId())) {
	                dates.put(m.getMovieId(), api.extractMovies(m.getMovieId()));
	            }
	        }
	    } catch (IOException e) {
	        log("Error while fetching movie schedule", e);
	        return;
	    } catch (XPathExpressionException e) {
	        log("Error while fetching movie schedule", e);
            return;
        }
	    for (MovieMonitor m : monitors) {
	        if (!movieCache.contains(m) && dates.containsKey(m.getMovieId())
	                && dates.get(m.getMovieId()).contains(m.getDate())) {
	            log("Sending email to " + m.getEmail());
	            movieCache.add(m);
	            StringBuilder str = new StringBuilder();
	            str.append("Movie \"" + m.getMovieId() + "\" is now available in schedule for " + m.getDate() + ".\r\n");
	            str.append("http://planeta-kino.com.ua/showtimes/");
	            try {
                    mailer.sendMail(m.getEmail(), m.getEmail(), 
                            "Movie " + m.getMovieId() + " is now available!", str.toString());
                } catch (Exception e) {
                    log("Error while sending email to " + m.getEmail(), e);
                } 
	                    
	        }
	    }
	}
	
	private String generateMessage(Map<Integer, List<Seat>> seats) {
	    StringBuilder str = new StringBuilder();
	    str.append("New seats are available!\r\n");
	    for (Entry<Integer, List<Seat>> e : seats.entrySet()) {
	        str.append("ShowId " + e.getKey() + ": ");
	        str.append(e.getValue().toString());
	        str.append("\r\n");
	        str.append(URL.replace("{id}", "" + e.getKey()) + "\r\n");
	    }
	    str.append("\r\n");
	    str.append("\r\n");
	    str.append("To stop this emails, please reply to this address (monitor318@gmail.com)");
	    return str.toString();
	}
	
	private void checkCache() {
	    log("Cache size:" + alarmed.size());
	    synchronized(alarmed) {
    	    long current = new Date().getTime();
    	    if (current - lastCheck > CACHE_CLEAR * 60 * 60 * 1000) {
    	        log("Clearing cache, before: " + alarmed.size());
    	        lastCheck = new Date().getTime();
    	        Iterator<Seat> it = alarmed.iterator();
    	        while(it.hasNext()) {
    	            Seat s = it.next();
    	            if (current - s.timeframe > CACHE_CLEAR * 60 * 60 * 1000)
    	                it.remove();
    	        }
    	        log("Cache cleared, after: " + alarmed.size());
    	    }
	    }
	}
}
