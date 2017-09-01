import kafka.consumer.KafkaStream;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by unknown_user on 4/23/2016.
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        int threads = 1;

        String sourceFolder = "C:/Users/unknown/Desktop/rawdata/traces/sanfranciscocabs";

        if(args.length >= 2) {
            sourceFolder = args[1];
        }

        TrafficLoadGenerator.generate(sourceFolder);

/*
        int threadNumber = 1000;

        for(int i = 0; i < threadNumber; i++) {
            (new Thread(new SensorDataProducer(10000, "Temperature"))).start();
        }
*/


        //(new Thread(new SensorDataProducer(10000, "Noise"))).start();
        //(new Thread(new SensorDataProducer(10000, "Co2"))).start();


      /*  SensorDataConsumerGroup example = new SensorDataConsumerGroup(Config.zookeeper, "TemperatureGroup","Temperature");
        example.run(threads);


        try {
            Thread.sleep(100000);
        } catch (InterruptedException ie) {

        }

        example.shutdown();*/
    }
}
