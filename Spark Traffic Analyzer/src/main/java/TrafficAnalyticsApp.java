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


public class TrafficAnalyticsApp {
    public static <T> Stream<T> stream(Iterable<T> in) {
        return StreamSupport.stream(in.spliterator(), false);
    }

    public static void main(final String[] args) throws Exception {
        String inputFile = args[0];
        String outputFile = args[1];

        String host = args[2];
        Integer port = Integer.parseInt(args[3]);
        String database = args[4];
        String user = args[5];
        String pass = args[6];
        String config = args[7];

        Logger log = LogManager.getRootLogger();
        log.setLevel(Level.INFO);
        log.info("Start");

        System.out.println("Start");

        // Create a Java Spark Context.
        SparkConf conf = new SparkConf().setAppName("wordCount");
        JavaSparkContext sc = new JavaSparkContext(conf);


        // Instantiate map matcher as broadcast variable in Spark Context (sc).
        Broadcast<BroadcastMatcher> matcher = sc.broadcast(new BroadcastMatcher().getInstance(host, port, database, user, pass, config));

        // Load trace data as RDD from CSV file asset of tuples:
        // (object-id: String, time: Long, position: Point)
        JavaRDD<Tuple3<String, Long, Point>> traces = sc.textFile("traces.csv").map(new Function<String, Tuple3<String, Long, Point>>() {
            @Override
            public Tuple3<String, Long, Point> call(String line) throws Exception {
                String[] split = line.split(",");
                return new Tuple3<String, Long, Point>(split[0], Long.parseLong(split[1]), new Point(Double.parseDouble(split[2]), Double.parseDouble(split[3])));
            }
        });

        JavaRDD<Tuple2<String, List<MatcherCandidate>>> matches = traces.groupBy(x -> x._1())
                .map(x ->
                {
                    List<MatcherSample> trip = stream(x._2())
                            .map(sample -> new MatcherSample(sample._1(), sample._2(), sample._3())).collect(Collectors.toList());
                    return new Tuple2<String, List<MatcherCandidate>>(x._1(), matcher.getValue().mmatch(trip).sequence());

                });

        System.out.println("Count lines " + matches.count());

        matches.saveAsObjectFile(outputFile);

        sc.stop();
    }

}

class BroadcastMatcher implements Serializable {
    private static final long serialVersionUID = 1L;
    private BroadcastMatcher instance = null;
    private Matcher matcher = null;

    public BroadcastMatcher getInstance(String host, int port, String name, String user, String pass, String configFile) throws JSONException {
        if (instance == null) {
            synchronized (this) {
                if (instance == null) { // initialize map matcher once per Executor (JVM process/cluster node)
                    PostGISReader reader = new PostGISReader(host, port, name, "bfmap_ways", user, pass, Configuration.read(new JSONObject(configFile)));
                    RoadMap map = RoadMap.Load(reader);

                    map.construct();

                    Dijkstra router = new Dijkstra<Road, RoadPoint>();
                    TimePriority cost = new TimePriority();
                    Geography spatial = new Geography();

                    instance = new BroadcastMatcher();
                    instance.matcher = new Matcher(map, router, cost, spatial);
                }
            }
        }

        return instance;
    }

    public MatcherKState mmatch(List<MatcherSample> samples) {
        return mmatch(samples, 0, 0);
    }

    public MatcherKState mmatch(List<MatcherSample> samples, double minDistance, int minInterval) {
        return this.instance.matcher.mmatch(samples, minDistance, minInterval);
    }
}