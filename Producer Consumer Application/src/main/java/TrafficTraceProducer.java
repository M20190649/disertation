import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.geotools.referencing.GeodeticCalculator;
import scala.collection.immutable.Seq$;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


// format of input file -> 37.75134 -122.39488 0 1213084687
public class TrafficTraceProducer implements Runnable {
    private Vector<String> file;

    final static String trafficTopic = "traffic";

    final static String eventType = "trafficSample";

    final static String eventSource = "SanFranciscoTraces";

    public static AtomicInteger samplesSent;

    public static AtomicInteger previousSentSamples;

    long testStartTime;

    long testDuration;

    public TrafficTraceProducer(Vector<String> file, long testStartTime, long testDuration) {
        this.file = file;
        this.testStartTime = testStartTime;
        this.testDuration = testDuration;
    }

    public void run() {

        //System.out.println("Staring traffic producer");

        Logger.getLogger("kafka").setLevel(Level.OFF);

        Properties props = new Properties();
        props.put("metadata.broker.list", Config.brokerList);
        props.put("serializer.class", "kafka.serializer.StringEncoder");
        props.put("partitioner.class", "SimplePartitioner");
        props.put("request.required.acks", "0");
        props.put("producer.type", "async");

        Random rnd = new Random();

        ProducerConfig config = new ProducerConfig(props);

        Producer<String, String> producer = new Producer<String, String>(config);

        try {

            double velocityX = 0;
            double velocityY = 0;
            double previousLat = 0;
            double previousLong = 0;
            long previousTimeStamp = 0;
            int previousOccupancy = -1;
            TaxiAction taxiAction = TaxiAction.Other;

            int currentDay = new java.util.Date().getDay();

            for(String line2: file) {

                String[] parts = line2.split(" ");

                double latitude = Double.parseDouble(parts[0]);
                double longitude = Double.parseDouble(parts[1]);
                int occupancy = Integer.parseInt(parts[2]);

                long timestamp = Long.parseLong(parts[3]);

                java.util.Date time = new java.util.Date((long)timestamp * 1000); // convert to milliseconds

                // We mark all trips as occuring today
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR,  time.getHours());
                cal.set(Calendar.MINUTE,  time.getMinutes());
                cal.set(Calendar.SECOND, time.getSeconds());

                // use it to generate a new id based on day and hour => we translate all car traces to the same hour, basically generating new traces, so we need a new carId so traces don't overlap
                String carId = UUID.randomUUID().toString() + ":" + (time.getDay() - cal.getTime().getDay());

                long translatedTimestamp = cal.getTimeInMillis(); // UTC epoch ?

                if(currentDay != cal.getTime().getDay()) {
                    velocityX = 0;
                    velocityY = 0;
                    previousLat = 0;
                    previousLong = 0;
                    previousTimeStamp = 0;
                    currentDay = cal.getTime().getDay();
                }

                try {
                    if(previousTimeStamp != 0) {
                        //Thread.sleep(Math.abs(translatedTimestamp - previousTimeStamp));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if(previousLat != 0 && previousLong != 0) {
                    GeodeticCalculator calc = new GeodeticCalculator();
                    calc.setStartingGeographicPoint(previousLong, previousLat);
                    calc.setDestinationGeographicPoint(previousLong, latitude);

                    int velocitySign = 1;
                    if(previousLat > latitude) {
                        velocitySign = -1;
                    }

                    velocityX = velocitySign * (calc.getOrthodromicDistance() / 1000) / ((double)(translatedTimestamp - previousTimeStamp) / (1000 * 3600));

                    calc = new GeodeticCalculator();
                    calc.setStartingGeographicPoint(previousLong, previousLat);
                    calc.setDestinationGeographicPoint(longitude, previousLat);

                    velocitySign = 1;
                    if(previousLong > longitude) {
                        velocitySign = -1;
                    }

                    velocityY = velocitySign * (calc.getOrthodromicDistance() / 1000) / (((double)translatedTimestamp - previousTimeStamp) / (1000 * 3600));

                    previousLat = latitude;
                    previousLong = longitude;
                    previousTimeStamp = translatedTimestamp;
                } else {
                    previousLat = latitude;
                    previousLong = longitude;
                    previousTimeStamp = translatedTimestamp;
                }

                if(previousOccupancy != -1) {
                    if(previousOccupancy != occupancy) {
                        if(occupancy == 0) {
                            taxiAction = TaxiAction.Dropoff;
                        } else {
                            taxiAction = TaxiAction.Pickup;
                        }
                    }
                }

                previousOccupancy = occupancy;

                TrafficObject trafficObject = new TrafficObject(carId,
                        latitude,
                        longitude,
                        occupancy,
                        translatedTimestamp,
                        velocityX,
                        velocityY,
                        taxiAction);

                String message = eventType + " " + eventSource + " " + trafficObject;

                for(int i = 0; i <= 10; i++) {

                    String partitioningKey = "192.168.1." + rnd.nextInt(255); // we simulate IPs as partitioning keys;
                    KeyedMessage<String, String> data = new KeyedMessage<String, String>(trafficTopic, partitioningKey, message);
                    producer.send(data);

                    TrafficTraceProducer.samplesSent.incrementAndGet();

                    if(System.currentTimeMillis() - testStartTime > testDuration) {
                        producer.close();
                        return;
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        producer.close();
    }
}