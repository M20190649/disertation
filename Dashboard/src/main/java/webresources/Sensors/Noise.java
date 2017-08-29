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
@RequestMapping("/noise")
public class Noise {

    private static final double refLat = 44.3369102;
    private static final double refLong = 25.950686;
    private static final double tileSize = 500; // meters

    @RequestMapping(value = "/count", method = RequestMethod.GET)
    public ResponseEntity<?> count(@RequestParam Map<String, String> queryParams) {

        MongoClient mongo = new MongoClient( "192.168.1.131" , 27017 );
        DB db = mongo.getDB("Noise");
        DBCollection collection = db.getCollection("Samples");

        return new ResponseEntity<>(collection.count(), HttpStatus.OK);
    }

    @RequestMapping(value = "/latestAggregate", method = RequestMethod.GET)
    public ResponseEntity<?> latestAggregate(@RequestParam Map<String, String> queryParams) {

        MongoClient mongo = new MongoClient( "192.168.1.131" , 27017 );
        DB db = mongo.getDB("Noise");
        DBCollection collection = db.getCollection("Aggregates");

        BasicDBObject order = new BasicDBObject();
        order.append("_id", -1);

        DBCursor cursor  = collection.find().sort(order);

        return new ResponseEntity<>(cursor.next(), HttpStatus.OK);
    }


    @RequestMapping(value = "/tiles", method = RequestMethod.GET)
    public ResponseEntity<?> tiles(@RequestParam Map<String, String> queryParams) {

        MongoClient mongo = new MongoClient( "34.233.214.65" , 27017 );
        DB db = mongo.getDB("Noise");
        DBCollection collection = db.getCollection("NoiseTiles");

        List<DBObject> results = new ArrayList<DBObject>();

        DBCursor cursor = collection.find();
        try {
            while(cursor.hasNext()) {
                BasicDBObject obj = (BasicDBObject) cursor.next();
                double tileX = obj.getDouble("lat");
                double tileY = obj.getDouble("long");

                GeodeticCalculator calc = new GeodeticCalculator();

                // Get left lower corner of tile
                calc.setStartingGeographicPoint(refLat, refLong);
                calc.setDirection(0, tileY * tileSize);
                Point2D t1 = calc.getDestinationGeographicPoint();

                calc.setStartingGeographicPoint(t1);
                calc.setDirection(90, tileX * tileSize);
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

                obj.put("lat1", llCorner.getX());
                obj.put("long1", llCorner.getY());

                obj.put("lat2", rlCorner.getX());
                obj.put("long2", rlCorner.getY());

                obj.put("lat3", luCorner.getX());
                obj.put("long3", luCorner.getY());

                obj.put("lat4", ruCorner.getX());
                obj.put("long4", ruCorner.getY());

                results.add(obj);
            }
        } finally {
            cursor.close();
        }


        return new ResponseEntity<>(results, HttpStatus.OK);
    }
}