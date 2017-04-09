/**
 * Created by unknown_user on 4/23/2016.
 */
public class Main {

    public static void main(String[] args) {
        String zooKeeper = "192.168.1.131:2181";
        String groupId = "group1";
        String topic = "noise";
        int threads = 2;

        (new Thread(new TestProducer(100000))).start();


        ConsumerGroup example = new ConsumerGroup(zooKeeper, groupId, topic);
        example.run(threads);


        try {
            Thread.sleep(100000);
        } catch (InterruptedException ie) {

        }


        example.shutdown();
    }
}
