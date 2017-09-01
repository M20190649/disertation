package webresources.Sensors;

// Imports ...

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.*;
import com.mongodb.client.MongoCursor;
import org.geotools.referencing.GeodeticCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import webresources.Locations.LocationDao;
import webresources.Locations.LocationResource;
import webresources.Security.AuthorizationProvider;
import webresources.UsersLocations.UserLocationDao;
import webresources.UsersLocations.UserLocationResources;

import javax.servlet.http.HttpServletRequest;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sensordata")
public class SensorDataBaseController {

    private static final double refLat = 37.704009;
    private static final double refLong = -122.509851;
    private static final double tileSize = 500; // meters

    private static final int resoucesLimit = 500;

    @RequestMapping(value = "/{sensorType}/count", method = RequestMethod.GET)
    public ResponseEntity<?> count(@PathVariable("sensorType") String sensorType, @RequestParam Map<String, String> queryParams) {

        MongoClient mongo = new MongoClient( Config.mongoIp , 27017 );
        DB db = mongo.getDB("DashboardAnalyticsDatabase");
        DBCollection collection = db.getCollection(StringUtils.capitalize(sensorType) + "_Samples");

        mongo.close();

        return new ResponseEntity<>(collection.count(), HttpStatus.OK);
    }

    @RequestMapping(value = "/{sensorType}/latestAggregate", method = RequestMethod.GET)
    public ResponseEntity<?> latestAggregate(@PathVariable("sensorType") String sensorType, @RequestParam Map<String, String> queryParams) {
        ;
        MongoClient mongo = new MongoClient( Config.mongoIp , 27017 );
        DB db = mongo.getDB("DashboardAnalyticsDatabase");
        DBCollection collection = db.getCollection(StringUtils.capitalize(sensorType) + "_Aggregates");

        BasicDBObject order = new BasicDBObject();
        order.append("_id", -1);

        DBCursor cursor  = collection.find().sort(order);

        DBObject latestAggregate = cursor.next();

        mongo.close();

        return new ResponseEntity<>(latestAggregate, HttpStatus.OK);
    }


    @RequestMapping(value = "/{sensorType}/tiles", method = RequestMethod.GET)
    public ResponseEntity<?> tiles(@PathVariable("sensorType") String sensorType, @RequestParam Map<String, String> queryParams) {

        MongoClient mongo = new MongoClient( Config.mongoIp , 27017 );
        DB db = mongo.getDB("DashboardAnalyticsDatabase");
        DBCollection collection = db.getCollection(StringUtils.capitalize(sensorType) + "_Tiles");

        int limit = resoucesLimit;
        if(queryParams.get("limit") != null) {
            limit = Integer.parseInt(queryParams.get("limit"));
        }


        List<DBObject> results = new ArrayList<DBObject>();

        BasicDBObject order = new BasicDBObject();
        order.append("_id", -1);

        DBCursor cursor = collection.find().sort(order).limit(limit);

        try {
            while(cursor.hasNext()) {
                BasicDBObject obj = (BasicDBObject) cursor.next();
                double tileX = obj.getDouble("lat");
                double tileY = obj.getDouble("long");

                GeodeticCalculator calc = new GeodeticCalculator();

                // Get left lower corner of tile
                calc.setStartingGeographicPoint(refLong, refLat);
                calc.setDirection(0, tileX * tileSize);
                Point2D t1 = calc.getDestinationGeographicPoint();

                calc.setStartingGeographicPoint(t1);
                calc.setDirection(90, tileY * tileSize);
                Point2D llCorner = calc.getDestinationGeographicPoint();

                // Get right lower corner of tile
                calc.setStartingGeographicPoint(llCorner);
                calc.setDirection(90, tileSize);
                Point2D rlCorner = calc.getDestinationGeographicPoint();

                // Get left upper corner of tile
                calc.setStartingGeographicPoint(llCorner);
                calc.setDirection(0, tileSize);
                Point2D luCorner = calc.getDestinationGeographicPoint();

                // Get right upper corner of tile
                calc.setStartingGeographicPoint(luCorner);
                calc.setDirection(90, tileSize);
                Point2D ruCorner = calc.getDestinationGeographicPoint();

                obj.put("lat1", llCorner.getY());
                obj.put("long1", llCorner.getX());

                obj.put("lat2", rlCorner.getY());
                obj.put("long2", rlCorner.getX());

                obj.put("lat3", luCorner.getY());
                obj.put("long3", luCorner.getX());

                obj.put("lat4", ruCorner.getY());
                obj.put("long4", ruCorner.getX());

                results.add(obj);
            }
        } finally {
            cursor.close();
            mongo.close();
        }

        mongo.close();

        return new ResponseEntity<>(results, HttpStatus.OK);
    }
}