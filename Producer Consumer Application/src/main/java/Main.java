import kafka.consumer.KafkaStream;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by unknown_user on 4/23/2016.
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {

        String sourceFolder = "C:/Users/unknown/Desktop/rawdata/traces/sanfranciscocabs";

        String sensorType = "Temperature";
        long runtimeMillis = 60000;
        if(args.length >= 1) {
            sensorType = args[0];
        }

        if(args.length >= 2) {
            runtimeMillis = Long.parseLong(args[1]);
        }

        if(args.length >= 3) {
            sourceFolder = args[2];
        }

        if(sensorType == "Traffic") {
            try {
                // > 1000 samplesPerSecond recommend due to timming and synchronization issues
                TrafficLoadGenerator.generate(sourceFolder, 10000, System.currentTimeMillis(), 60000, 60000);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            try {
                SensorDataLoadGenerator.generate(sensorType,  runtimeMillis);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        }


      /*  SensorDataConsumerGroup example = new SensorDataConsumerGroup(Config.zookeeper, "TemperatureGroup","Temperature");
        example.run(threads);


        try {
            Thread.sleep(100000);
        } catch (InterruptedException ie) {

        }

        example.shutdown();*/
    }
}
