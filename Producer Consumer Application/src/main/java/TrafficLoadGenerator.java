import java.io.File;


public class TrafficLoadGenerator {

    static void generate(String sourceFolderPath) {
        File folder = new File(sourceFolderPath);
        File[] listOfFiles = folder.listFiles();

        for (int i = 0; i < listOfFiles.length; i++) {
            (new Thread(new TrafficTraceProducer(listOfFiles[i].getAbsolutePath()))).start();
        }
    }
}
