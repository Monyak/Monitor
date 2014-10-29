package imax.monitor.server;


public class Seat {

    public final int id;
    public final int row;
    public final int seat;
    public final long timeframe;
    
    public Seat(int id, int row, int seat, long time) {
        this.row = row;
        this.seat = seat;
        this.id = id;
        this.timeframe = time;
    }

    @Override
    public String toString() {
        return "(" + row + "," + seat + ")";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        result = prime * result + row;
        result = prime * result + seat;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Seat other = (Seat) obj;
        if (id != other.id)
            return false;
        if (row != other.row)
            return false;
        if (seat != other.seat)
            return false;
        return true;
    }
    
    
}
