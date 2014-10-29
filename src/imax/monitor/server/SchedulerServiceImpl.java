package imax.monitor.server;

import imax.monitor.shared.Monitor;

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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class SchedulerServiceImpl extends MonitorServiceImpl {

	private static final long serialVersionUID = -1290583620502335408L;
	
	private static final String URL = "https://cabinet.planeta-kino.com.ua/hall/?show_id={id}&theatre_id=imax-kiev&lang=ua";

    private static final AdapterAPI api = new AdapterAPI();
    private static final Mailer mailer = new Mailer();
    private static final Random rand = new Random();
    
    private static final int CACHE_CLEAR = 2; // 2 hours;
    
    private static HashSet<Seat> alarmed = new HashSet<>();
    private static long lastCheck;
	
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		log("Starting checks");
		List<Monitor> monitors = getMonitors();
		HashMap<Integer, Map<String, List<Monitor>>> ids = new HashMap<>();
		HashMap<String, Boolean> emails = new HashMap<String, Boolean>();
		for (Monitor m : monitors) {
		    emails.put(m.getEmail(), false);
		    for (int i = 0; i < m.getIds().length; i++) {
		        if (!ids.containsKey(m.getIds()[i])) {
		            ids.put(m.getIds()[i], new HashMap<String, List<Monitor>>());
		        }
		        Map<String, List<Monitor>> map = ids.get(m.getIds()[i]);
		        if (!map.containsKey(m.getEmail())) {
		            map.put(m.getEmail(), new ArrayList<Monitor>());
		        }
		        map.get(m.getEmail()).add(m);
		    }
		}
		
		checkCache();
		
		log("Got " + ids.size() + " ids, " + monitors.size() + " monitors and " + emails.size() + " users.");
		
		Map<String, Map<Integer, List<Seat>>> available = new HashMap<>();
		
		for (Entry<Integer, Map<String, List<Monitor>>> entry : ids.entrySet()) {
		    List<Seat> seats = null;
		    try {
		        seats = api.getAvailableSeats(entry.getKey());
		    } catch (IllegalStateException e) {
		        // todo remove from checks
		        log("Id " + entry.getKey() + " is not working", e);
		        continue;
		    } catch (IOException e) {
		        log("Error while fetching data for " + entry.getKey(), e);
		        continue;
		    }
		    Map<String, List<Monitor>> map = entry.getValue();
		    for (String email : map.keySet()) {
    		    for (Monitor m : map.get(email)) {
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
                    log("Error while sending email", e);
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
