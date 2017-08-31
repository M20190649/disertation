import java.util.*;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;

public class SensorDataProducer implements Runnable {
    private int m_nEvents;
    private String m_nSensorType;

    public SensorDataProducer(int a_nEvents, String a_nSensorType) {
        m_nEvents = a_nEvents;
        m_nSensorType = a_nSensorType;
    }

    public void run() {
        long events = m_nEvents;
        Random rnd = new Random();

        System.out.println("Staring producer with: " + events);

        Properties props = new Properties();
        props.put("metadata.broker.list", Config.brokerList);
        props.put("serializer.class", "kafka.serializer.StringEncoder");
        props.put("partitioner.class", "SimplePartitioner");
        props.put("request.required.acks", "1");

        System.out.println("Staring producer with: " + events);

        ProducerConfig config = new ProducerConfig(props);

        Producer<String, String> producer = new Producer<String, String>(config);

        System.out.println("Staring producer with: " + events);

        for (long nEvents = 0; nEvents < events; nEvents++) {
            Calendar cal = Calendar.getInstance();

            private static final double refLat = 37.704009;
            private static final double refLong = -122.509851;
            37.8408570, -122.3386317

            String msg;
            if(nEvents % 100 == 0) {
                msg = String.format("%s %s %s %f %f %f",
                        m_nSensorType,
                        RandomStringUtils.randomAlphanumeric(10),
                        "" + (cal.getTimeInMillis() / 1000),
                        RandomUtils.nextFloat((float) 37.704009, (float) 37.8408570),
                        RandomUtils.nextFloat((float) -122.509851, (float) -122.3386317),
                        RandomUtils.nextFloat(120, 150));
            } else if(nEvents % 200 == 0) {
                msg = String.format("%s %s %s %f %f %f",
                        m_nSensorType,
                        RandomStringUtils.randomAlphanumeric(10),
                        "" + (cal.getTimeInMillis() / 1000),
                        RandomUtils.nextFloat((float) 37.704009, (float) 37.8408570),
                        RandomUtils.nextFloat((float) -122.509851, (float) -122.3386317),
                        RandomUtils.nextFloat(60, 100));
            }
            else
            {
                msg = String.format("%s %s %s %f %f %f",
                        m_nSensorType,
                        RandomStringUtils.randomAlphanumeric(10),
                        "" + (cal.getTimeInMillis() / 1000),
                        RandomUtils.nextFloat((float) 37.704009, (float) 37.8408570),
                        RandomUtils.nextFloat((float) -122.509851, (float) -122.3386317),
                        RandomUtils.nextFloat(0, 100));
            }

            String partitioningKey = "192.168.1." + rnd.nextInt(255); // we simulate IPs as partitioning keys;
            KeyedMessage<String, String> data = new KeyedMessage<String, String>(m_nSensorType, partitioningKey, msg);
            producer.send(data);

            System.out.println("Sent: " + msg);
        }

        producer.close();
    }
}