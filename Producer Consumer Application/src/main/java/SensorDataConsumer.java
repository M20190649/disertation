import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;

public class SensorDataConsumer implements Runnable {
    private KafkaStream m_stream;
    private int m_threadNumber;
    private String m_sensorType;

    public SensorDataConsumer(KafkaStream a_stream, int a_threadNumber, String a_sensorType) {
        m_threadNumber = a_threadNumber;
        m_stream = a_stream;
        m_sensorType = a_sensorType;
    }

    public void run() {

        MongoClient mongo = new MongoClient( Config.mongoIp , 27017 );
        DB db = mongo.getDB("DashboardAnalyticsDatabase");
        DBCollection collection = db.getCollection(m_sensorType + "_Samples");

        ConsumerIterator<byte[], byte[]> it = m_stream.iterator();
        while (it.hasNext()) {
            String msg = new String(it.next().message());

            System.out.println("Thread " + m_threadNumber + "received : " + msg);

            String[] parts = msg.split(" ");

            BasicDBObject doc = new BasicDBObject()
                    .append("evenType", parts[0])
                    .append("eventSources", parts[1])
                    .append("date", parts[2])
                    .append("latitude", Float.parseFloat(parts[3]))
                    .append("longitude", Float.parseFloat(parts[4]))
                    .append("value", Float.parseFloat(parts[5]));

            collection.insert(doc);
        }

        mongo.close();

        System.out.println("Shutting down Thread: " + m_threadNumber);
    }
}