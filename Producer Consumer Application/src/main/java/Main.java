/**
 * Created by unknown_user on 4/23/2016.
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        String groupId = "group1";
        String topic = "noise";
        int threads = 2;

        String sourceFolder = "C:/Users/unknown/Desktop/rawdata/traces/sanfranciscocabs";

        if(args.length >= 2) {
            sourceFolder = args[1];
        }

        TrafficLoadGenerator.generate(sourceFolder);

        /*(new Thread(new TestProducer(100000))).start();


        ConsumerGroup example = new ConsumerGroup(Config.zookeeper, groupId, topic);
        example.run(threads);


        try {
            Thread.sleep(100000);
        } catch (InterruptedException ie) {

        }


        example.shutdown();*/
    }
}
