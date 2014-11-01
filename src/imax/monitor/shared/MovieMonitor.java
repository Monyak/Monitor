package imax.monitor.shared;

import java.io.Serializable;

public class MovieMonitor implements Serializable, IMonitor {

    private static final long serialVersionUID = -7066373004107386296L;
    
    
    private int movieId;
    private String email;
    
    public MovieMonitor() {
        
    }
    
    public MovieMonitor(int id, String mail) {
        this.movieId = id;
        this.email = mail;
    }

    public int getMovieId() {
        return movieId;
    }

    public void setMovieId(int movieId) {
        this.movieId = movieId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((email == null) ? 0 : email.hashCode());
        result = prime * result + movieId;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof MovieMonitor))
            return false;
        MovieMonitor other = (MovieMonitor) obj;
        if (email == null) {
            if (other.email != null)
                return false;
        } else if (!email.equals(other.email))
            return false;
        if (movieId != other.movieId)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "MovieMonitor [movieId=" + movieId + ", email=" + email + "]";
    }
    
    
}
