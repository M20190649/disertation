
import java.util.*;
import org.apache.spark.api.java.*;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaPairReceiverInputDStream;
import org.apache.spark.streaming.kafka.KafkaUtils;

import org.geotools.referencing.GeodeticCalculator;
import scala.Tuple2;

import org.apache.spark.sql.*;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.catalyst.ScalaReflection;

import org.apache.spark.api.java.function.Function;
// Import factory methods provided by DataTypes.
import org.apache.spark.sql.types.DataTypes;
// Import StructType and StructField
import org.apache.spark.sql.types.StructType;
import org.apache.spark.sql.types.StructField;
// Import Row.
import org.apache.spark.sql.Row;
// Import RowFactory.
import org.apache.spark.sql.RowFactory;


public class DashboardAnalyticsApp {

  private static final double refLat = 44.3369102;
  private static final double refLong = 25.950686;
  private static final double tileSize = 500; // meters



  public static void main(String[] args) {
    Config config = new Config();

    SparkConf conf = new SparkConf().setAppName("Dashboard Analytics App");
    JavaSparkContext sc = new JavaSparkContext(conf);

    JavaStreamingContext jssc = new JavaStreamingContext(sc, new Duration(10000));

    SQLContext sqlContext = new org.apache.spark.sql.SQLContext(sc);
    final Map options = new HashMap();
    options.put("host", config.mongoDatabaseHost + ":" + config.mongoDatabasePort);
    options.put("database", "DashboardAnalyticsDatabase");
    options.put("collection", config.application + "_Aggregates");

    final Map options2 = new HashMap();
    options2.put("host", config.mongoDatabaseHost + ":" + config.mongoDatabasePort);
    options2.put("database", "DashboardAnalyticsDatabase");
    options2.put("collection", config.application + "_Tiles");
    options2.put("splitKey", "tileKey");
    options2.put("splitKeyType", "string");


    // The schema is encoded in a string
    String schemaString = "average min max count";

    // Generate the schema based on the string of schema
      List<StructField> fields = new ArrayList<StructField>();
    for (String fieldName: schemaString.split(" ")) {
      fields.add(DataTypes.createStructField(fieldName, DataTypes.DoubleType, true));
    }
    final StructType schema = DataTypes.createStructType(fields);


    //
    // The schema is encoded in a string
    String schemaString2 = "tileKey lat long avg samples";

    // Generate the schema based on the string of schema
    List<StructField> fields2 = new ArrayList<StructField>();
    for (String fieldName: schemaString2.split(" ")) {
      if(fieldName.equals("tileKey")) {
        fields2.add(DataTypes.createStructField(fieldName, DataTypes.StringType, true));
      } else {
        fields2.add(DataTypes.createStructField(fieldName, DataTypes.DoubleType, true));
      }
    }
    final StructType schema2 = DataTypes.createStructType(fields2);

    //
    Map<String, Integer> topicMap = new HashMap<String, Integer>();
    topicMap.put("noise", 2);

    JavaPairReceiverInputDStream<String, String> messages =
            KafkaUtils.createStream(jssc, config.kafkaHost + ":" + config.kafkaPort, config.kafkaGroup, topicMap);

    JavaDStream<Double> samples = messages.map(new Function<Tuple2<String, String>, Double>() {
      @Override
      public Double call(Tuple2<String, String> tuple2) {
        String[] parts = tuple2._2().split(";");

        return Double.parseDouble(parts[5]);
      }
    });

    samples.foreachRDD(new Function<JavaRDD<Double>, Void>() {
      public Void call(JavaRDD<Double> rdd) {

        if(!rdd.partitions().isEmpty()) {
          SQLContext sqlContext = SQLContext.getOrCreate(rdd.context());

          JavaDoubleRDD samples = rdd.mapToDouble(new org.apache.spark.api.java.function.DoubleFunction<Double>() {
            public double call(Double x) {
              return x;
            }
          });

          double avg = samples.mean();
          double min = samples.min();

          double max = samples.max();
          double count = samples.count();

          Row row = RowFactory.create(avg, min, max, count);

          JavaSparkContext sc = new JavaSparkContext(rdd.context());
          JavaRDD<Row> rowRDD = sc.parallelize(Arrays.asList(row));

          DataFrame temperatureDataFrame = sqlContext.createDataFrame(rowRDD, schema);

          temperatureDataFrame.write().format("com.stratio.datasource.mongodb").mode("append").options(options).save();
        }

        return null;
      }

    });


    // tile aggregation
    JavaPairDStream<String, Double> pairs = messages.mapToPair(new PairFunction<Tuple2<String, String>, String, Double>() {
      @Override
      public Tuple2<String, Double> call(Tuple2<String, String> tuple2) {
        String[] parts = tuple2._2().split(";");

        double latitude = Double.parseDouble(parts[3]);
        double longitude = Double.parseDouble(parts[4]);

        GeodeticCalculator gc = new GeodeticCalculator();

        gc.setStartingGeographicPoint(refLat, refLong);
        gc.setDestinationGeographicPoint(latitude, refLong);

        double distance = gc.getOrthodromicDistance();

        int totalmetersX = (int) distance;
        int tileIdX = (int)totalmetersX / (int)tileSize;

        gc = new GeodeticCalculator();

        gc.setStartingGeographicPoint(refLat, refLong);
        gc.setDestinationGeographicPoint(refLat, longitude);

        distance = gc.getOrthodromicDistance();
        int totalmetersY = (int) distance;
        int tileIdY = (int)totalmetersY / (int)tileSize;

        String key = tileIdX + ":" + tileIdY;

        return new Tuple2<String, Double>(key, Double.parseDouble(parts[5]));
      }
    });

    JavaPairDStream<String, Tuple2<Double, Integer>> intermediaryPairs = pairs.mapValues(new Function<Double, Tuple2<Double, Integer>>() {
      @Override
      public Tuple2<Double, Integer> call(Double val) {
        return new Tuple2<Double, Integer>(val, 1);
      }
    });


    JavaPairDStream<String, Tuple2<Double, Integer>> reducedCounts = intermediaryPairs.reduceByKey(new Function2<Tuple2<Double, Integer>, Tuple2<Double, Integer>, Tuple2<Double, Integer>>() {
      @Override
      public Tuple2<Double, Integer> call(final Tuple2<Double, Integer> value0, final Tuple2<Double, Integer> value1) {
        return new Tuple2(value0._1() + value1._1(), value0._2() + value1._2());
      }
    });

    reducedCounts.foreachRDD(new Function<JavaPairRDD<String, Tuple2<Double, Integer>>, Void>() {
      public Void call(JavaPairRDD<String, Tuple2<Double, Integer>> rdd) {

        if(!rdd.partitions().isEmpty()) {

          SQLContext sqlContext = SQLContext.getOrCreate(rdd.context());

          List<String> keys = rdd.map(new Function<Tuple2<String, Tuple2<Double, Integer>>, String>() {
            @Override
            public String call(Tuple2<String, Tuple2<Double, Integer>> tuple2) {
              return tuple2._1();
            }
          }).collect();

          String key = keys.isEmpty() ? "999:999" : keys.get(0);

          if(keys.isEmpty()) {
            return null;
          }

          Map<String, Tuple2<Double, Integer>> tiles = rdd.collectAsMap();

          for (Map.Entry<String, Tuple2<Double, Integer>> tile : tiles.entrySet())
          {
            String key2 = tile.getKey();
            String[] parts = key2.split(":");
            double tileIdX = Double.parseDouble(parts[0]);
            double tileIdY = Double.parseDouble(parts[1]);

            Row row = RowFactory.create(key2, tileIdX, tileIdY,
                    tile.getValue()._1() / tile.getValue()._2(),
                    (double)tile.getValue()._2());

            JavaSparkContext sc = new JavaSparkContext(rdd.context());
            JavaRDD<Row> rowRDD = sc.parallelize(Arrays.asList(row));

            DataFrame temperatureDataFrame = sqlContext.createDataFrame(rowRDD, schema2);

            temperatureDataFrame.write().format("com.stratio.datasource.mongodb").mode("append").options(options2).save();
          }
        }
        return null;
      }
    });


    jssc.start();
    // Wait for 10 seconds then exit. To run forever call without a timeout
    jssc.awaitTermination();
    // Stop the streaming context
    jssc.stop();
  }



}