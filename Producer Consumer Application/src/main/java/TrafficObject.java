enum TaxiAction {
    Other(0),
    Pickup(1),
    Dropoff(2);

    private int value;

    private TaxiAction(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}

public class TrafficObject {
    public String carId;
    public double latitude;
    public double longitude;
    public int occupancy;
    public long timestamp;
    public double velocityX;
    public double velocityY;
    public TaxiAction taxiAction;

    public TrafficObject(String carId, double latitude, double longitude, int occupancy, long timestamp, double velocityX, double velocityY, TaxiAction taxiAction) {
        this.carId = carId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.occupancy = occupancy;
        this.timestamp = timestamp;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.taxiAction = taxiAction;
    }

    public String toString() {
        return String.format("%s %d %f %f %f %f %d %d", carId, timestamp, latitude, longitude, velocityX, velocityY, occupancy, taxiAction.getValue());
    }
}