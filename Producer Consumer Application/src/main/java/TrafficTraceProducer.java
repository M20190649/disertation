import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import java.util.Random;

// format of input file -> 37.75134 -122.39488 0 1213084687
public class TrafficTraceProducer implements Runnable {
    private String m_nFileLocation;

    public TrafficTraceProducer(String a_nFileLocation) {
        m_nFileLocation = a_nFileLocation;
    }

    public void run() {

        long unixTime = System.currentTimeMillis() / 1000L;
        long unixTimeOffset = 0; // used to translate read timestamps to current timestamps

        try {
            BufferedReader br = new BufferedReader(new FileReader(m_nFileLocation))

            String line;
            while ((line = br.readLine()) != null) {
                // process the line.
            }
        } catch (FileNotFoundException fne) {
            System.out.println("File Not Found" + fne.getStackTrace());
        } catch (IOException ioe) {
            System.out.println("IOException" + ioe.getStackTrace());
        }


        long events = m_nEvents;
        Random rnd = new Random();

        System.out.println("Staring producer with: " + events);

        Properties props = new Properties();
        props.put("metadata.broker.list", "192.168.1.131:9092");
        props.put("serializer.class", "kafka.serializer.StringEncoder");
        props.put("partitioner.class", "SimplePartitioner");
        props.put("request.required.acks", "1");

        System.out.println("Staring producer with: " + events);

        ProducerConfig config = new ProducerConfig(props);

        Producer<String, String> producer = new Producer<String, String>(config);

        System.out.println("Staring producer with: " + events);

        for (long nEvents = 0; nEvents < events; nEvents++) {
            long runtime = new Date().getTime();
            String ip = "192.168.1.131";

            String msg;
            if(nEvents % 100 == 0) {
                msg = String.format("%s; %s; %s; %f; %f; %f",
                        "noise",
                        RandomStringUtils.randomAlphanumeric(10),
                        (new Date()).toString(),
                        RandomUtils.nextFloat((float) 44.5005622, (float) 44.5028918),
                        RandomUtils.nextFloat((float) 26.095008, (float) 26.1294057),
                        RandomUtils.nextFloat(120, 150));
            } else if(nEvents % 200 == 0) {
                msg = String.format("%s; %s; %s; %f; %f; %f",
                        "noise",
                        RandomStringUtils.randomAlphanumeric(10),
                        (new Date()).toString(),
                        RandomUtils.nextFloat((float) 4.4316092, (float) 44.4330008),
                        RandomUtils.nextFloat((float) 26.0977631, (float) 26.10281537),
                        RandomUtils.nextFloat(60, 100));
            }
            else
            {
                msg = String.format("%s; %s; %s; %f; %f; %f",
                        "noise",
                        RandomStringUtils.randomAlphanumeric(10),
                        (new Date()).toString(),
                        RandomUtils.nextFloat((float) 44.3332918, (float) 44.543616),
                        RandomUtils.nextFloat((float) 25.9563351, (float) 26.2386826),
                        RandomUtils.nextFloat(0, 100));
            }

            KeyedMessage<String, String> data = new KeyedMessage<String, String>("noise", ip, msg);
            producer.send(data);

            System.out.println("Sent: " + msg);
        }

        producer.close();
    }
}