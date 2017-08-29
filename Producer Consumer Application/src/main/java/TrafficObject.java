public class TrafficObject {
    public String carId;
    public double latitude;
    public double longitude;
    public int occupancy;
    public long timestamp;
    public double velocityX;
    public double velocityY;

    public TrafficObject(String carId, double latitude, double longitude, int occupancy, long timestamp, double velocityX, double velocityY) {
        this.carId = carId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.occupancy = occupancy;
        this.timestamp = timestamp;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
    }

    public String toString() {
        return String.format("%s %d %f %f %f %f %d", carId, timestamp, latitude, longitude, velocityX, velocityY, occupancy);
    }
}