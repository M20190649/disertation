package webresources.Sensors;

// Imports ...

import com.mongodb.*;
import org.geotools.referencing.GeodeticCalculator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/traffic")
public class Traffic {

    private static final double refLat = 37.704009;
    private static final double refLong = -122.509851;
    private static final double tileSize = 500; // meters
    private static final int resoucesLimit = 500;

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


    @RequestMapping(value = "/aggregates", method = RequestMethod.GET)
    public ResponseEntity<?> averageVelocities(@RequestParam Map<String, String> queryParams) {

        MongoClient mongo = new MongoClient( "34.233.214.65" , 27017 );
        DB db = mongo.getDB("DashboardAnalyticsDatabase");
        DBCollection collection = db.getCollection("Traffic_Aggregates");

        List<DBObject> results = new ArrayList<DBObject>();

        int limit = resoucesLimit;
        if(queryParams.get("limit") != null) {
            limit = Integer.parseInt(queryParams.get("limit"));
        }

        BasicDBObject order = new BasicDBObject();
        order.append("_id", -1);

        DBCursor cursor = collection.find().sort(order).limit(limit);


        try {
            while(cursor.hasNext()) {
                BasicDBObject obj = (BasicDBObject) cursor.next();
                int tileX = Integer.parseInt(obj.getString("key").split(":")[0]);
                int tileY = Integer.parseInt(obj.getString("key").split(":")[1]);

     /*           GeodeticCalculator calc = new GeodeticCalculator();

                // Get left lower corner of tile
                calc.setStartingGeographicPoint(refLong, refLat);
                calc.setDirection(0, tileY * tileSize);
                Point2D t1 = calc.getDestinationGeographicPoint();

                calc.setStartingGeographicPoint(t1);
                calc.setDirection(90, tileX * tileSize);
                Point2D llCorner = calc.getDestinationGeographicPoint();


                calc.setStartingGeographicPoint(llCorner);
                calc.setDirection(90, tileSize / 2);
                Point2D center = calc.getDestinationGeographicPoint();

                // Get left upper corner of tile
                calc.setStartingGeographicPoint(center);
                calc.setDirection(0, tileSize / 2);
                center = calc.getDestinationGeographicPoint();*/

                Point2D center =  new Point2D.Double(obj.getDouble("averagePositionX"), obj.getDouble("averagePositionY"));

                obj.put("centerArrowX", center.getX());
                obj.put("centerArrowY", center.getY());

                GeodeticCalculator calc = new GeodeticCalculator();

                calc.setStartingGeographicPoint(center.getY(), center.getX());
                calc.setDirection(90, (obj.getDouble("avgVelocityX") / 100 ) * (tileSize / 2)); // some hack to get the arrow size
                Point2D velocityArrowHead = calc.getDestinationGeographicPoint();

                calc.setStartingGeographicPoint(velocityArrowHead.getX(), velocityArrowHead.getY());
                calc.setDirection(0, (obj.getDouble("avgVelocityY") / 100 ) * (tileSize / 2));
                velocityArrowHead = calc.getDestinationGeographicPoint();
                velocityArrowHead = new Point2D.Double(velocityArrowHead.getY(), velocityArrowHead.getX()); // The GeodeticCalculator works with longitude, latitude pairs


                obj.put("arrowHeadX", velocityArrowHead.getX());
                obj.put("arrowHeadY", velocityArrowHead.getY());

                results.add(obj);
            }
        } finally {
            cursor.close();
        }


        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @RequestMapping(value = "/taxiactionclusters", method = RequestMethod.GET)
    public ResponseEntity<?> pickupClusters(@RequestParam Map<String, String> queryParams) {

        MongoClient mongo = new MongoClient( "34.233.214.65" , 27017 );
        DB db = mongo.getDB("DashboardAnalyticsDatabase");
        DBCollection collection = db.getCollection("Traffic_Clusters_KMeans");

        TaxiAction taxiAction = TaxiAction.Other;
        if(queryParams.get("taxiaction") != null) {
            if(queryParams.get("taxiaction").equals("pickups")) {
                taxiAction = TaxiAction.Pickup;
            } else if(queryParams.get("taxiaction").equals("dropoffs")) {
                taxiAction = TaxiAction.Dropoff;
            }
        }

        BasicDBObject query = new BasicDBObject();
        query.put("taxiActionType", taxiAction.getValue());

        List<DBObject> results = new ArrayList<DBObject>();

        int numberOfCluster = Integer.parseInt(queryParams.get("numberOfClusters"));

        BasicDBObject order = new BasicDBObject();
        order.append("_id", -1);

        DBCursor cursor  = collection.find(query).sort(order).limit(numberOfCluster);

        try {
            while(cursor.hasNext()) {
                BasicDBObject obj = (BasicDBObject) cursor.next();
                results.add(obj);
            }
        } finally {
            cursor.close();
        }


        return new ResponseEntity<>(results, HttpStatus.OK);
    }

}