import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;
import org.geotools.referencing.GeodeticCalculator;
import scala.collection.immutable.Seq$;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


// format of input file -> 37.75134 -122.39488 0 1213084687
public class TrafficTraceProducer implements Runnable {
    private String m_nFileLocation;

    final static String trafficTopic = "traffic";

    final static String eventType = "trafficSample";

    final static String eventSource = "SanFranciscoTraces";

    public TrafficTraceProducer(String a_nFileLocation) {
        m_nFileLocation = a_nFileLocation;
    }

    public void run() {

        System.out.println("Staring traffic producer");

        Properties props = new Properties();
        props.put("metadata.broker.list", Config.brokerList);
        props.put("serializer.class", "kafka.serializer.StringEncoder");
        props.put("partitioner.class", "SimplePartitioner");
        props.put("request.required.acks", "1");

        ProducerConfig config = new ProducerConfig(props);

        Producer<String, String> producer = new Producer<String, String>(config);

        try {
            Vector<String> lines = new Vector<String>();
            BufferedReader br = new BufferedReader(new FileReader(m_nFileLocation));

            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }

            Collections.reverse(lines);

            double velocityX = 0;
            double velocityY = 0;
            double previousLat = 0;
            double previousLong = 0;
            long previousTimeStamp = 0;
            int previousOccupancy = -1;
            TaxiAction taxiAction = TaxiAction.Other;

            int currentDay = new java.util.Date().getDay();


            for(String line2: lines) {
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

                // use it to generate a new id based on day => we translate all car traces to the same day, basically generating new traces, so we need a new carId so traces don't overlap
                int day = time.getDay() - cal.getTime().getDay();

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
                        Thread.sleep(translatedTimestamp - previousTimeStamp);
                    }
                } catch (Exception e) {
                    System.out.println(e.getStackTrace());
                }



                if(previousLat != 0 && previousLong != 0) {
                    GeodeticCalculator calc = new GeodeticCalculator();
                    calc.setStartingGeographicPoint(previousLong, previousLat);
                    calc.setDestinationGeographicPoint(previousLong, latitude);

                    velocityX = (calc.getOrthodromicDistance() / 1000) / ((double)(translatedTimestamp - previousTimeStamp) / (1000 * 3600));

                    calc = new GeodeticCalculator();
                    calc.setStartingGeographicPoint(previousLong, previousLat);
                    calc.setDestinationGeographicPoint(longitude, previousLat);

                    velocityY = (calc.getOrthodromicDistance() / 1000) / (((double)translatedTimestamp - previousTimeStamp) / (1000 * 3600));

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

                TrafficObject trafficObject = new TrafficObject(UUID.randomUUID().toString() + ":" + day,
                        latitude,
                        longitude,
                        occupancy,
                        translatedTimestamp,
                        velocityX,
                        velocityY,
                        taxiAction);

                String message = eventType + " " + eventSource + " " + trafficObject;
                KeyedMessage<String, String> data = new KeyedMessage<String, String>(trafficTopic, "keyForPartitioning", message);
                producer.send(data);

            }
        } catch (FileNotFoundException fne) {
            System.out.println("File Not Found" + fne.getStackTrace());
        } catch (IOException ioe) {
            System.out.println("IOException" + ioe.getStackTrace());
        }

        producer.close();
    }
}