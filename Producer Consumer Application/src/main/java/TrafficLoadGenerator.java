import scala.Array;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


public class TrafficLoadGenerator {

    static void generate(String sourceFolderPath,
                         long testDuration) {

        System.out.println("Staring traffic load generation");

        File folder = new File(sourceFolderPath);
        File[] listOfFiles = folder.listFiles();

        TrafficTraceProducer.samplesSent = new AtomicInteger(0);
        TrafficTraceProducer.previousSentSamples = new AtomicInteger(0);
        ArrayList<Thread> threadList = new ArrayList<Thread>();

        Timer t = new Timer( );
        long startTime = System.currentTimeMillis();

        t.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                System.out.println("Traffic Samples/s: " + (TrafficTraceProducer.samplesSent.get() - TrafficTraceProducer.previousSentSamples.get()));
                TrafficTraceProducer.previousSentSamples.set(TrafficTraceProducer.samplesSent.get());

            }
        }, 0,1000);

        Vector<Vector<String>> files = new Vector<Vector<String>>();
        for (int i = 0; i < listOfFiles.length; i++) {
            Vector<String> lines = new Vector<String>();
            try {

                BufferedReader br = new BufferedReader(new FileReader(listOfFiles[i].getAbsolutePath()));

                String line;
                while ((line = br.readLine()) != null) {
                    lines.add(line);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            files.add(lines);

            Collections.reverse(lines);
        }

        System.out.println("Done reading files! " + new Date());

        for (int i = 0; i < listOfFiles.length; i++) {
            threadList.add(new Thread(new TrafficTraceProducer(files.get(i), startTime, testDuration)));
        }


        for (int i = 0; i < listOfFiles.length; i++) {
            threadList.get(i).start();
        }

        for (int i = 0; i < listOfFiles.length; i++) {
            try {
                threadList.get(i).join();
                System.out.println("Thread " + i + " finally joined!!!");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        t.cancel();

        System.out.println("Total samples sent:" + TrafficTraceProducer.samplesSent);
        System.out.println("Ellapsed time: " + (System.currentTimeMillis() - startTime) / 1000);

    }
}
