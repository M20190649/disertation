package webresources.UtilsApis;

// Imports ...

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.*;
import com.vividsolutions.jts.geom.Coordinate;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.JTS;
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
@RequestMapping("/utils")
public class Utils {

    @RequestMapping(value = "/dist", method = RequestMethod.GET)
    public ResponseEntity<?> dist(@RequestParam Map<String, String> queryParams) {

        GeodeticCalculator gc = new GeodeticCalculator();

        double lat1 = Float.parseFloat(queryParams.get("lat1"));
        double long1 = Float.parseFloat(queryParams.get("long1"));
        double lat2 = Float.parseFloat(queryParams.get("lat2"));
        double long2 = Float.parseFloat(queryParams.get("long2"));


        gc.setStartingGeographicPoint(lat1, long1);
        gc.setDestinationGeographicPoint(lat2, long2);

        double distance = gc.getOrthodromicDistance();

        int totalmeters = (int) distance;
        int km = totalmeters / 1000;
        int meters = totalmeters - (km * 1000);
        float remaining_cm = (float) (distance - totalmeters) * 10000;
        remaining_cm = Math.round(remaining_cm);
        float cm = remaining_cm / 100;

        System.out.println("Distance = " + km + "km " + meters + "m " + cm + "cm");

        return new ResponseEntity<>("Distance = " + km + "km " + meters + "m " + cm + "cm", HttpStatus.OK);
    }

    @RequestMapping(value = "/newPoint", method = RequestMethod.GET)
    public ResponseEntity<?> newPoint(@RequestParam Map<String, String> queryParams) {

        GeodeticCalculator gc = new GeodeticCalculator();

        double lat1 = Float.parseFloat(queryParams.get("lat1"));
        double long1 = Float.parseFloat(queryParams.get("long1"));
        double dist = Float.parseFloat(queryParams.get("dist"));
        double angle = Float.parseFloat(queryParams.get("angle"));


        GeodeticCalculator calc = new GeodeticCalculator();
        // mind, this is lon/lat
        calc.setStartingGeographicPoint(lat1, long1);
        calc.setDirection(angle, dist);
        Point2D dest = calc.getDestinationGeographicPoint();
        System.out.println("Longitude: " + dest.getX() + " Latitude: " + dest.getY());

        return new ResponseEntity<>("Longitude: " + dest.getX() + " Latitude: " + dest.getY(), HttpStatus.OK);
    }

}