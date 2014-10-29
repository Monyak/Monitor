package imax.monitor.shared;

import java.io.Serializable;
import java.util.Arrays;

public class Monitor implements Serializable {
  
    private static final long serialVersionUID = -2913628940087205755L;
    
    
    private int[] ids;
    private int[] rows; // [2] - interval
    private int[] seats; // same
    private String email; // email to notify
    
    public Monitor() {
        
    }
    
    public Monitor(int[] ids, int[] rows, int[] seats, String email) {
        if (ids == null || ids.length == 0)
            throw new IllegalArgumentException("Incorrect ids");
        if (rows == null || rows.length != 2)
            throw new IllegalArgumentException("Incorrect rows");
        if (seats == null || seats.length != 2)
            throw new IllegalArgumentException("Incorrect seats");
        if (email == null || email.isEmpty())
            throw new IllegalArgumentException("Incorrect email");
        this.ids = copyArray(ids);
        this.rows = copyArray(rows);
        this.seats = copyArray(seats);
        this.email = email;
    }
    
    private int[] copyArray(int[] array) {
        int[] copy = new int[array.length];
        System.arraycopy(array, 0, copy, 0, array.length);
        return copy;
    }
    
    public int[] getIds() {
        return ids;
    }
    
    public String getEmail() {
        return email;
    }
    
    public int[] getRows() {
        return rows;
    }
    
    public int[] getSeats() {
        return seats;
    }
    
    public boolean isRequired(int row, int seat) {
        return row >= rows[0] && row <= rows[1]
                && seat >= seats[0] && seat <= seats[1];
    }
    
    @Override
    public String toString() {
        return "ShowIds:" + Arrays.toString(ids) + ";rows:" + rows[0] + "-" + rows[1]
                + ";seats:" + seats[0] + "-" + seats[1] + ";email=" + email;
    }
}
