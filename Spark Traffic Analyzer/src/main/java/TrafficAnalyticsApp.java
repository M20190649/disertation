/**
 * Illustrates a wordcount in Java
 */

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.lang.Iterable;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.bmwcarit.barefoot.matcher.Matcher;
import com.bmwcarit.barefoot.matcher.MatcherCandidate;
import com.bmwcarit.barefoot.matcher.MatcherKState;
import com.bmwcarit.barefoot.matcher.MatcherSample;
import com.bmwcarit.barefoot.road.PostGISReader;
import com.bmwcarit.barefoot.roadmap.Road;
import com.bmwcarit.barefoot.roadmap.RoadMap;
import com.bmwcarit.barefoot.roadmap.RoadPoint;
import com.bmwcarit.barefoot.roadmap.TimePriority;
import com.bmwcarit.barefoot.spatial.Geography;
import com.bmwcarit.barefoot.topology.Dijkstra;
import com.esri.core.geometry.Point;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.broadcast.Broadcast;
import org.json.JSONException;
import org.json.JSONObject;
import scala.Tuple2;

import org.apache.commons.lang.StringUtils;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import scala.Tuple3;
import scala.Tuple4;
import scala.collection.Iterator;


public class TrafficAnalyticsApp {
    public static <T> Stream<T> stream(Iterable<T> in) {
        return StreamSupport.stream(in.spliterator(), false);
    }

    public static void main(final String[] args) throws Exception, JSONException   {
        String inputFile = args[0];
        String outputFile = args[1];

        String host = args[2];
        Integer port = Integer.parseInt(args[3]);
        String database = args[4];
        String user = args[5];
        String pass = args[6];
        String config = args[7];
        String traceFile = args[8];

        Logger log = LogManager.getRootLogger();
        log.setLevel(Level.INFO);
        log.info("Start");

        System.out.println("Start");

        // Create a Java Spark Context.
        SparkConf conf = new SparkConf().setAppName("TrafficAnalyticsApp");
        JavaSparkContext sc = new JavaSparkContext(conf);

        String configFile = "";
        JavaPairRDD<String, String> rdd = sc.wholeTextFiles(config);

        configFile = rdd.first()._2();

        log.info("Config file: " + configFile);

        // Instantiate map matcher as broadcast variable in Spark Context (sc).
        Broadcast<BroadcastMatcher> matcher = sc.broadcast(new BroadcastMatcher(host, port, database, user, pass, configFile ));

        // Load trace data as RDD from CSV file asset of tuples:
        // (object-id: String, time: Long, position: Point)
        JavaRDD<Tuple3<String, Long, Point>> traces = sc.textFile(traceFile).map(new Function<String, Tuple3<String, Long, Point>>() {
            @Override
            public Tuple3<String, Long, Point> call(String line) throws Exception {
                String[] split = line.split(",");
                return new Tuple3<String, Long, Point>(split[0], Long.parseLong(split[1]), new Point(Double.parseDouble(split[2]), Double.parseDouble(split[3])));
            }
        });


        log.info("Contents of mapped traces:\n");

        sc.textFile(traceFile).map(new Function<String, Tuple3<String, Long, Point>>() {
                @Override
                public Tuple3<String, Long, Point> call(String line) throws Exception {
                    String[] split = line.split(",");
                    return new Tuple3<String, Long, Point>(split[0], Long.parseLong(split[1]), new Point(Double.parseDouble(split[2]), Double.parseDouble(split[3])));
                }
        }).collect().forEach(stringLongPointTuple3 -> System.out.println(stringLongPointTuple3));

        JavaRDD<Tuple2<String, List<MatcherSample>>> matches = traces.groupBy(x -> x._1())
                .map(x ->
                {
                    List<MatcherSample> trip = stream(x._2())
                            .map(sample -> new MatcherSample(sample._1(), sample._2(), sample._3())).collect(Collectors.toList());

                    //System.out.println("Trip length: " + trip.size());

                   //return new Tuple2<String, List<MatcherCandidate>>(x._1(), matcher.getValue().mmatch(trip).sequence());
                    return new Tuple2<String, List<MatcherSample>>(x._1(), trip);
                }
                );


        matches.collect().forEach(stringLongPointTuple3 -> System.out.println(stringLongPointTuple3));

        //JavaRDD<String> ouput = matches.map(v1 -> helper(v1._1(), v1._2()));

       /* stringListTuple2 -> {
            String temp = "";

            for(MatcherCandidate m: stringListTuple2._2()) {
                temp += stringListTuple2._1() + '\n' + m.toJSON();
            }

            return temp;
        }*/

        System.out.println("Count lines " + matches.count());

        log.info("Matches count: " + matches.count());

        //ouput.saveAsObjectFile(outputFile);

        sc.stop();
    }

    public static String helper(String a, List<MatcherCandidate> lst) {
        String temp = "";

        if(lst != null) {
            for(MatcherCandidate m: lst) {
                try {
                    temp += a + '\n' + m.toJSON();
                } catch(Exception ex) {

                }
            }
        }

        return temp;
    }

}

final class BroadcastMatcherSingleton {
    public static BroadcastMatcherSingleton instance = null;
    public static Matcher matcher = null;

    public static BroadcastMatcherSingleton getInstance(String host, int port, String name, String user, String pass, String configFile) throws JSONException {
        if (instance == null) {
            synchronized (BroadcastMatcherSingleton.class) {
                if (instance == null) { // initialize map matcher once per Executor (JVM process/cluster node)

                    PostGISReader reader = new PostGISReader(host, port, name, "bfmap_ways", user, pass, Configuration.read(new JSONObject(configFile)));
                    RoadMap map = RoadMap.Load(reader);

                    map.construct();

                    System.out.println(map.size());

                    Dijkstra router = new Dijkstra<Road, RoadPoint>();
                    TimePriority cost = new TimePriority();
                    Geography spatial = new Geography();

                    instance = new BroadcastMatcherSingleton();
                    instance.matcher = new Matcher(map, router, cost, spatial);
                }
            }
        }

        return instance;
    }
}

class BroadcastMatcher implements Serializable {
    private static final long serialVersionUID = 1L;

    String host;
    int port;
    String name;
    String user;
    String pass;
    String configFile;

    public BroadcastMatcher(String host, int port, String name, String user, String pass, String configFile) {
        this.host = host;
        this.port = port;
        this.name = name;
        this.user = user;
        this.pass = pass;
        this.configFile = configFile;
    }

    public MatcherKState mmatch(List<MatcherSample> samples) throws JSONException  {
        return mmatch(samples, 0, 0);
    }

    public MatcherKState mmatch(List<MatcherSample> samples, double minDistance, int minInterval) throws JSONException  {

        for(MatcherSample sample: samples) {
            System.out.println("Print sample");
            System.out.println(sample.toJSON());
        }

        return BroadcastMatcherSingleton.getInstance(host, port, name, user, pass, configFile).matcher.mmatch(samples, minDistance, minInterval);
    }
}