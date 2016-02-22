package cs.lmu.StreamCam.services;

import android.location.Location;

import java.util.Date;
import java.util.LinkedList;

/**
 * Created by juanscarrillo on 2/21/16.
 */
public class TravelLog {

    private LinkedList <TimedLocation> log;
    private Date lastTimeStamp;
    private Location lastLocation;
    private Location initialLocation;
    private Date initialTimeStamp;
    private String lastAddress;

    public TravelLog() {

    }

    public void add(Location location, String address){
        Date currentTime = new Date();
        if(this.log.size() == 0) {
            this.initialTimeStamp = currentTime;
            this.initialLocation = location;
        }
        if(lastAddress.equals(address) || lastLocation.equals(location)) {
            return;
        }
        TimedLocation currentLocation = new TimedLocation(currentTime, location, address);
        this.log.add(currentLocation);
        this.lastLocation = location;
        this.lastAddress = address;
        this.lastTimeStamp = currentTime;
    }

    public int length() {
        return this.log.size();
    }

    public String toString() {
        String result = "";
        for(TimedLocation location : this.log) {
            result += location.toString() + "\n";
        }
        return result;
    }

    private class TimedLocation{

        private Date date;
        private String address;
        private Location location;

        public TimedLocation(Date date, Location location, String streetAddress) {
            this.date = date;
            this.address = streetAddress;
            this.location = location;
        }

        public TimedLocation(Date date, Location location) {
            this.date = date;
            this.address = "";
            this.location = location;
        }

        public String toString() {
            String latitudeString = String.valueOf(this.location.getLatitude());
            String longititudeString = String.valueOf(this.location.getLongitude());
            String result = "Date: " +  date.toString() +  ";"
                          + "(Latitude, Longitude): "
                          + "(" +  latitudeString  + ", " + longititudeString + "); "
                          + "Street Adress: " +  this.address;
            return result;

        }
    }
}
