import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by unknown on 9/2/2017.
 */
public class SensorDataLoadGenerator {
    static void generate(
            final String sensorType,
            long testDuration) {

        System.out.println("Staring " + sensorType + " load generation");

        SensorDataProducer.samplesSent = new AtomicInteger(0);
        SensorDataProducer.previousSentSamples = new AtomicInteger(0);
        ArrayList<Thread> threadList = new ArrayList<Thread>();

        Timer t = new Timer( );
        long startTime = System.currentTimeMillis();

        t.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                System.out.println(sensorType + " Samples/s: " + (SensorDataProducer.samplesSent.get() - SensorDataProducer.previousSentSamples.get()));
                SensorDataProducer.previousSentSamples.set(SensorDataProducer.samplesSent.get());

            }
        }, 0,1000);

        int nrThreads = 1000;
        for (int i = 0; i < nrThreads; i++) {
            threadList.add(new Thread(new SensorDataProducer(sensorType , System.currentTimeMillis(), testDuration)));
        }

        for (int i = 0; i < nrThreads; i++) {
            threadList.get(i).start();
        }

        for (int i = 0; i < nrThreads; i++) {
            try {
                threadList.get(i).join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        t.cancel();

        System.out.println("[" + sensorType + "]" + "Total samples sent:" + SensorDataProducer.samplesSent);
        System.out.println("[" + sensorType + "]" + "Ellapsed time: " + (System.currentTimeMillis() - startTime) / 1000);
    }
}
