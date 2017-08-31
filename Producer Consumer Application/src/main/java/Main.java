/**
 * Created by unknown_user on 4/23/2016.
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        int threads = 2;

        String sourceFolder = "C:/Users/unknown/Desktop/rawdata/traces/sanfranciscocabs";

        if(args.length >= 2) {
            sourceFolder = args[1];
        }

        //TrafficLoadGenerator.generate(sourceFolder);

        (new Thread(new SensorDataProducer(10000, "Temperature"))).start();


        SensorDataConsumerGroup example = new SensorDataConsumerGroup(Config.zookeeper, "TemperatureGroup","Temperature");
        example.run(threads);


        try {
            Thread.sleep(100000);
        } catch (InterruptedException ie) {

        }

        //example.shutdown();
    }
}
