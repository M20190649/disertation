package webresources.Sensors;

// Imports ...

import com.mongodb.*;
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

    @RequestMapping(value = "/averageVelocities", method = RequestMethod.GET)
    public ResponseEntity<?> averageVelocities(@RequestParam Map<String, String> queryParams) {

        MongoClient mongo = new MongoClient( "34.228.141.44" , 27017 );
        DB db = mongo.getDB("Traffic");
        DBCollection collection = db.getCollection("statistics");

        List<DBObject> results = new ArrayList<DBObject>();

        DBCursor cursor = collection.find();
        try {
            while(cursor.hasNext()) {
                BasicDBObject obj = (BasicDBObject) cursor.next();
                double latitude = obj.getDouble("lat");
                double longitude = obj.getDouble("long");
                double averageVelocity = obj.getDouble("averageVelocity");

                obj.put("latitude", latitude);
                obj.put("averageVelocity", longitude);
                obj.put("averageVelocity", averageVelocity);

                results.add(obj);
            }
        } finally {
            cursor.close();
        }

        return new ResponseEntity<>(results, HttpStatus.OK);
    }
}