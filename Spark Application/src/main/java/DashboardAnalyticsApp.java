
import java.awt.geom.Point2D;
import java.util.*;
import java.util.Vector;

import org.apache.avro.echo.Pong;
import org.apache.spark.api.java.*;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.mllib.clustering.StreamingKMeans;
import org.apache.spark.mllib.linalg.*;
import org.apache.spark.sql.types.IntegerType;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaPairReceiverInputDStream;
import org.apache.spark.streaming.dstream.DStream;
import org.apache.spark.streaming.kafka.KafkaUtils;

import org.geotools.referencing.GeodeticCalculator;
import scala.Array;
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
import scala.Tuple3;
import scala.Tuple4;

import javax.measure.quantity.Velocity;
import javax.vecmath.Point2d;
import javax.vecmath.Vector2d;


enum TaxiAction {
  Other(0),
  Pickup(1),
  Dropoff(2);

  private int value;

  private TaxiAction(int value) {
    this.value = value;
  }

  public static TaxiAction fromValue(int value) {
    for (TaxiAction type : TaxiAction.values()) {
      if (type.getValue() == value) {
        return type;
      }
    }
    return null;
  }

  public int getValue() {
    return value;
  }
}


/*

Noise event - evenType,eventSources,latitude,longitude,value
Car event - eventType, eventSource, carId, timestamp, latitude, longitude, velocity, occupancy
*/

public class DashboardAnalyticsApp {

/*  private static final double refLat = 44.3369102;
  private static final double refLong = 25.950686;*/
  private static final double refLat = 37.704009;
  private static final double refLong = -122.509851;
  private static final double tileSize = 500; // meters



  public static void main(String[] args) {

    TrafficProcessing();
    /*


    Config config = new Config();

    SparkConf conf = new SparkConf().setAppName("Dashboard Analytics App");
    JavaSparkContext sc = new JavaSparkContext(conf);

    JavaStreamingContext jssc = new JavaStreamingContext(sc, new Duration(10000));

    SQLContext sqlContext = new org.apache.spark.sql.SQLContext(sc);
    final Map AggregatesMongoConfig = new HashMap();
    AggregatesMongoConfig.put("host", config.mongoDatabaseHost + ":" + config.mongoDatabasePort);
    AggregatesMongoConfig.put("database", "DashboardAnalyticsDatabase");
    AggregatesMongoConfig.put("collection", config.application + "_Aggregates");

    final Map TilesMongoConfig = new HashMap();
    TilesMongoConfig.put("host", config.mongoDatabaseHost + ":" + config.mongoDatabasePort);
    TilesMongoConfig.put("database", "DashboardAnalyticsDatabase");
    TilesMongoConfig.put("collection", config.application + "_Tiles");
    TilesMongoConfig.put("splitKey", "tileKey");
    TilesMongoConfig.put("splitKeyType", "string");


    Map<String, Integer> topicMap = new HashMap<String, Integer>();
    topicMap.put("noise", 2); // number of kafka partitions to consume

    // <K, V> - K = kafka message id, V = message itself
    JavaPairReceiverInputDStream<String, String> messages =
            KafkaUtils.createStream(jssc, config.kafkaHost + ":" + config.kafkaPort, config.kafkaGroup, topicMap);

    JavaDStream<Double> samples = GetSampleValues(messages);

    ComputeOveralStatisticsAndWriteToMongo(samples, AggregatesMongoConfig);

    // Duplicate stream?
    JavaPairDStream<String, Double> pairs = AggregateTiles(messages);

    // preprocess to add number of samples
    JavaPairDStream<String, Tuple2<Double, Integer>> intermediaryPairs = pairs.mapValues(new Function<Double, Tuple2<Double, Integer>>() {
      @Override
      public Tuple2<Double, Integer> call(Double val) {
        return new Tuple2<Double, Integer>(val, 1);
      }
    });

    // tileId -> (sum(sampleValues), countSamples))
    JavaPairDStream<String, Tuple2<Double, Integer>> reducedCounts = intermediaryPairs.reduceByKey(new Function2<Tuple2<Double, Integer>, Tuple2<Double, Integer>, Tuple2<Double, Integer>>() {
      @Override
      public Tuple2<Double, Integer> call(final Tuple2<Double, Integer> value0, final Tuple2<Double, Integer> value1) {
        return new Tuple2(value0._1() + value1._1(), value0._2() + value1._2());
      }
    });


    ComputeTileAveragesAndWriteToMongo(reducedCounts, TilesMongoConfig);

    jssc.start();
    // Wait for 10 seconds then exit. To run forever call without a timeout
    jssc.awaitTermination();
    // Stop the streaming context
    jssc.stop();*/
  }

  static void TrafficProcessing() {
    final int countCarVectorDirections = 2;

    Config config = new Config();

    SparkConf conf = new SparkConf().setAppName("Dashboard Analytics App");
    JavaSparkContext sc = new JavaSparkContext(conf);

    JavaStreamingContext jssc = new JavaStreamingContext(sc, new Duration(10000));

    final Map TilesMongoConfig = new HashMap();
    TilesMongoConfig.put("host", config.mongoDatabaseHost + ":" + config.mongoDatabasePort);
    TilesMongoConfig.put("database", "DashboardAnalyticsDatabase");
    TilesMongoConfig.put("collection", "Traffic_Aggregates");
    TilesMongoConfig.put("splitKey", "key");
    TilesMongoConfig.put("splitKeyType", "string");


    Map<String, Integer> topicMap = new HashMap<String, Integer>();
    topicMap.put("traffic", 2); // number of kafka partitions to consume

    JavaPairReceiverInputDStream<String, String> messages =
            KafkaUtils.createStream(jssc, config.kafkaHost + ":" + config.kafkaPort, config.kafkaGroup, topicMap);

    messages.cache(); // ? check if this stream is duplicated

    // key = tileIdX + ":" + tileIdY;
    JavaPairDStream<String, Tuple3<Point2d, Vector2d, Integer>> perTileAggregationOfTraffic = messages.mapToPair(new PairFunction<Tuple2<String, String>, String, Tuple3<Point2d, Vector2d, Integer>>() {
      @Override
      public Tuple2<String, Tuple3<Point2d, Vector2d, Integer>> call(Tuple2<String, String> tuple2) {
        String[] parts = tuple2._2().split(" ");

        //eventType, eventSource, carId, timestamp, latitude, longitude, velocityX, velocityY, occupancy
        String carId = parts[2];
        Long timeStamp = Long.parseLong(parts[3]);
        double latitude = Double.parseDouble(parts[4]);
        double longitude = Double.parseDouble(parts[5]);
        Vector2d velocity = new Vector2d(Double.parseDouble(parts[6]), Double.parseDouble(parts[7]));
        Point2d position = new Point2d(latitude, longitude);

        GeodeticCalculator gc = new GeodeticCalculator();

        gc.setStartingGeographicPoint(refLong, refLat);
        gc.setDestinationGeographicPoint(longitude, refLat);

        double distance = gc.getOrthodromicDistance();

        int totalmetersX = (int) distance;
        int tileIdX = (int)totalmetersX / (int)tileSize;

        gc = new GeodeticCalculator();

        gc.setStartingGeographicPoint(refLong, refLat);
        gc.setDestinationGeographicPoint(refLong, latitude);

        distance = gc.getOrthodromicDistance();
        int totalmetersY = (int) distance;
        int tileIdY = (int)totalmetersY / (int)tileSize;

        String key = tileIdX + ":" + tileIdY;

        return new Tuple2<>(key, new Tuple3<>(position, velocity, 1)); // 1 is used for counting purposes
      }
    });

    JavaPairDStream<String, Iterable<Tuple3<Point2d, Vector2d, Integer>>> aggregationOfTraffic = perTileAggregationOfTraffic
            .groupByKey()
            .mapValues(new Function<Iterable<Tuple3<Point2d, Vector2d, Integer>>, Iterable<Tuple3<Point2d, Vector2d, Integer>>>() {
              @Override
              public Iterable<Tuple3<Point2d, Vector2d, Integer>> call(Iterable<Tuple3<Point2d, Vector2d, Integer>> carSamples) throws Exception {
                double angleBetweenProjectionSegment = 360 / countCarVectorDirections;
                List<Tuple3<Point2d, Vector2d, Integer>> aggregationsByDirection = new ArrayList<>();


                for (Tuple3<Point2d, Vector2d, Integer> carSample : carSamples) {
                  if (aggregationsByDirection.size() < countCarVectorDirections) {
                    aggregationsByDirection.add(carSample);
                  } else {
                    int minAngleAverageVelocityCarSampleId = 0; // de ce a dat eroare la -1 ? ;
                    double minAngleValue = 360;

                    for (int i = 0; i < aggregationsByDirection.size(); i++) {
                      Vector2d averageVelocity = aggregationsByDirection.get(i)._2();
                      if (angleVectors(averageVelocity, carSample._2()) < minAngleValue &&
                              angleVectors(averageVelocity, carSample._2()) <= angleBetweenProjectionSegment) {
                        minAngleAverageVelocityCarSampleId = i;
                        minAngleValue = angleVectors(averageVelocity, carSample._2());
                      }
                    }

                    Vector2d averageVelocity = aggregationsByDirection.get(minAngleAverageVelocityCarSampleId)._2();
                    Point2d averagePosition = aggregationsByDirection.get(minAngleAverageVelocityCarSampleId)._1();

                    int newCount = aggregationsByDirection.get(minAngleAverageVelocityCarSampleId)._3() + 1;
                    double velocityX = averageVelocity.x * (newCount - 1) + carSample._2().x;
                    velocityX /= newCount;
                    double velocityY = averageVelocity.y * (newCount - 1)  + carSample._2().y;
                    velocityY /= newCount;

                    double positionX = averagePosition.x * (newCount - 1) + carSample._1().x;
                    positionX /= newCount;
                    double positionY = averagePosition.y * (newCount - 1)  + carSample._1().y;
                    positionY /= newCount;

                    Vector2d updatedAverageVelocity = new Vector2d(velocityX, velocityY);
                    Point2d updatedPosition = new Point2d(positionX, positionY);

                    aggregationsByDirection.set(minAngleAverageVelocityCarSampleId, new Tuple3<>(updatedPosition, updatedAverageVelocity, newCount));
                  }
                }

                return aggregationsByDirection;
              }
            });


    // The schema is encoded in a string
    // key = tilekey:directon number to avoid collisions
    String schemaString2 = "key averagePositionX averagePositionY avgVelocityX avgVelocityY CountCarsForAverageVelocity";

    // Generate the schema based on the string of schema
    List<StructField> fields2 = new ArrayList<StructField>();
    for (String fieldName: schemaString2.split(" ")) {
      if(fieldName.equals("key")) {
        fields2.add(DataTypes.createStructField(fieldName, DataTypes.StringType, true));
      }
      else if (fieldName.equals("CountCarsForAverageVelocity")) {
        fields2.add(DataTypes.createStructField(fieldName, DataTypes.IntegerType, true));
      } else {
        fields2.add(DataTypes.createStructField(fieldName, DataTypes.DoubleType, true));
      }
    }
    final StructType schema2 = DataTypes.createStructType(fields2);

    aggregationOfTraffic.foreachRDD(new Function<JavaPairRDD<String, Iterable<Tuple3<Point2d, Vector2d, Integer>>>, Void>() {
      public Void call(JavaPairRDD<String, Iterable<Tuple3<Point2d, Vector2d, Integer>>> rdd) {

        if(!rdd.partitions().isEmpty()) {

          SQLContext sqlContext = SQLContext.getOrCreate(rdd.context());

          List<String> keys = rdd.map(new Function<Tuple2<String,Iterable<Tuple3<Point2d, Vector2d,Integer>>>, String>() {
            @Override
            public String call(Tuple2<String, Iterable<Tuple3<Point2d, Vector2d, Integer>>> v1) throws Exception {
              return v1._1();
            }
          }).collect();

          if(keys.isEmpty()) {
            return null;
          }

          Map<String, Iterable<Tuple3<Point2d, Vector2d, Integer>>> tiles = rdd.collectAsMap();

          for (Map.Entry<String, Iterable<Tuple3<Point2d, Vector2d, Integer>>> tile : tiles.entrySet())
          {
            int directionNumber = 0;

            for(Tuple3<Point2d, Vector2d, Integer> aggregationTuple: tile.getValue()) {

              String key = tile.getKey() + ":" + directionNumber;
              directionNumber++;

              // Average per tile
              Row row = RowFactory.create(key,
                      aggregationTuple._1().x,
                      aggregationTuple._1().y,
                      aggregationTuple._2().x,
                      aggregationTuple._2().y,
                      aggregationTuple._3());

              JavaSparkContext sc = new JavaSparkContext(rdd.context());
              JavaRDD<Row> rowRDD = sc.parallelize(Arrays.asList(row));

              DataFrame dataFrame = sqlContext.createDataFrame(rowRDD, schema2);

              dataFrame.write().format("com.stratio.datasource.mongodb").mode(SaveMode.Append).options(TilesMongoConfig).save();
            }
          }
        }
        return null;
      }
    });


    JavaPairDStream<String, String> taxiPickups = messages.filter(new Function<Tuple2<String, String>, Boolean>() {
      @Override
      public Boolean call(Tuple2<String, String> v1) throws Exception {
        String[] parts = v1._2().split(" ");
        TaxiAction taxiAction = TaxiAction.fromValue(Integer.parseInt(parts[8]));

        return taxiAction == TaxiAction.Pickup;
      }
    });

    DStream<org.apache.spark.mllib.linalg.Vector> taxiPickupsFeatures = taxiPickups.map(new Function<Tuple2<String, String>, org.apache.spark.mllib.linalg.Vector>() {
      @Override
      public org.apache.spark.mllib.linalg.Vector call(Tuple2<String, String> v1) throws Exception {
        String[] parts = v1._2().split(" ");

        double latitude = Double.parseDouble(parts[4]);
        double longitude = Double.parseDouble(parts[5]);

        org.apache.spark.mllib.linalg.Vector vector = Vectors.dense(latitude, longitude);

        return vector;
      }
    }).dstream();

    final int numberOfClusters = 3;
    org.apache.spark.mllib.linalg.Vector[] initialClusterCenters = new org.apache.spark.mllib.linalg.Vector[numberOfClusters];
    double[] weights = new double[numberOfClusters];
    Random random = new Random();

    for(int i = 0; i < numberOfClusters; i++) {
      int angle = random.nextInt(360) - 180;

      GeodeticCalculator calc = new GeodeticCalculator();

      // Get left lower corner of tile
      calc.setStartingGeographicPoint(refLong, refLat);
      calc.setDirection(angle, 10000); // 10 km
      Point2D clusterCenter = calc.getDestinationGeographicPoint();

      initialClusterCenters[i] = Vectors.dense(clusterCenter.getX(), clusterCenter.getY());
      weights[i] = 1;
    }

    StreamingKMeans streamingKMeans = new StreamingKMeans()
            .setK(3)
            .setInitialCenters(initialClusterCenters, weights)
            .setDecayFactor(0.5);
    streamingKMeans.trainOn(taxiPickupsFeatures);

    org.apache.spark.mllib.linalg.Vector[] clusterCenters = streamingKMeans.model().clusterCenters();

    final Map KMeansClustersMongoConfig = new HashMap();
    KMeansClustersMongoConfig.put("host", config.mongoDatabaseHost + ":" + config.mongoDatabasePort);
    KMeansClustersMongoConfig.put("database", "DashboardAnalyticsDatabase");
    KMeansClustersMongoConfig.put("collection", "Traffic_Clusters_KMeans");

    // The schema is encoded in a string
    // key = tilekey:directon number to avoid collisions
    String schemaKMeansClustersString = "latitude longitude taxiActionType";

    // Generate the schema based on the string of schema
    List<StructField> KMeansClusterFields = new ArrayList<StructField>();
    for (String fieldName: schemaKMeansClustersString.split(" ")) {
    if (fieldName.equals("taxiActionType")) {
      KMeansClusterFields.add(DataTypes.createStructField(fieldName, DataTypes.IntegerType, true));
      } else {
      KMeansClusterFields.add(DataTypes.createStructField(fieldName, DataTypes.DoubleType, true));
      }
    }

    final StructType schemaKMeansClusters = DataTypes.createStructType(KMeansClusterFields);

    ArrayList<Row> rows = new ArrayList<>();
    for(org.apache.spark.mllib.linalg.Vector clusterCenter: clusterCenters) {
      // Average per tile
      Row row = RowFactory.create(clusterCenter.toArray()[0], clusterCenter.toArray()[1], TaxiAction.Pickup.getValue());
      rows.add(row);
    }

    JavaRDD<Row> rowRDD = sc.parallelize(rows);

    SQLContext sqlContext = new SQLContext(sc);
    DataFrame dataFrame = sqlContext.createDataFrame(rowRDD, schemaKMeansClusters);

    dataFrame.write().format("com.stratio.datasource.mongodb").mode(SaveMode.Append).options(KMeansClustersMongoConfig).save();

    jssc.start();
    // Wait for 10 seconds then exit. To run forever call without a timeout
    jssc.awaitTermination();
    // Stop the streaming context
    jssc.stop();
  }


  static JavaDStream<Double> GetSampleValues(JavaPairReceiverInputDStream<String, String> inputDStream) {
    return inputDStream.map(new Function<Tuple2<String, String>, Double>() {
      @Override
      public Double call(Tuple2<String, String> tuple2) {
        String[] parts = tuple2._2().split(";");

        return Double.parseDouble(parts[5]);
      }
    });
  }

  static void ComputeOveralStatisticsAndWriteToMongo(JavaDStream<Double> samples, final Map options){

    // The schema is encoded in a string
    String schemaString = "average min max count";

    // Generate the schema based on the string of schema
    List<StructField> fields = new ArrayList<StructField>();
    for (String fieldName: schemaString.split(" ")) {
      fields.add(DataTypes.createStructField(fieldName, DataTypes.DoubleType, true));
    }
    final StructType schema = DataTypes.createStructType(fields);

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
  }

  // Return - tileX:tileY - sample value
  static JavaPairDStream<String, Double> AggregateTiles(JavaPairReceiverInputDStream<String, String> messages) {
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

    return pairs;
  }

  static void ComputeTileAveragesAndWriteToMongo(JavaPairDStream<String, Tuple2<Double, Integer>> reducedCounts, final Map options) {
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

            // Average per tile
            Row row = RowFactory.create(key2, tileIdX, tileIdY,
                    tile.getValue()._1() / tile.getValue()._2(),
                    (double)tile.getValue()._2());

            JavaSparkContext sc = new JavaSparkContext(rdd.context());
            JavaRDD<Row> rowRDD = sc.parallelize(Arrays.asList(row));

            DataFrame temperatureDataFrame = sqlContext.createDataFrame(rowRDD, schema2);

            temperatureDataFrame.write().format("com.stratio.datasource.mongodb").mode("append").options(options).save();
          }
        }
        return null;
      }
    });
  }


  static double angleVectors(Vector2d vec1, Vector2d vec2) {
    return Math.toDegrees(vec1.angle(vec2));
  }





}