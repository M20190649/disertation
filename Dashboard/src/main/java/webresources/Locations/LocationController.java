package webresources.Locations;

// Imports ...

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import webresources.*;
import webresources.Security.AuthorizationProvider;
import webresources.Users.UserDao;
import webresources.Users.UserResource;
import webresources.UsersLocations.UserLocationDao;
import webresources.UsersLocations.UserLocationResources;
import webresources.Utilities.Utils;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@EnableConfigurationProperties(ApplicationConfiguration.class)
@RestController
@RequestMapping("/locations")
public class LocationController {

    private final LocationDao locationDao;
    private final UserLocationDao userLocationDao;
    private final UserDao userDao;
    private final AuthorizationProvider authorizationProvider;
    private final ApplicationConfiguration appConfig;

    private final String defaultImg = "Default Image.png";


    // Auto-injected shared entity manager.
    // http://docs.spring.io/spring/docs/current/spring-framework-reference/html/orm.html
    @PersistenceContext
    private EntityManager em;

    @Autowired
    public LocationController(LocationDao locationDao,
                              UserLocationDao userLocationDao,
                              UserDao userDao,
                              AuthorizationProvider authorizationProvider,
                              ApplicationConfiguration appConfig) {
        this.locationDao = locationDao;
        this.userLocationDao = userLocationDao;
        this.userDao = userDao;
        this.authorizationProvider = authorizationProvider;
        this.appConfig = appConfig;
    }

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<?> create(@RequestBody LocationResource receivedLocation) {

        Date date = new Date();
        receivedLocation.createDate = new Timestamp(date.getTime());
        receivedLocation.lastUpdateDate = receivedLocation.createDate;

        locationDao.save(receivedLocation);

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @RequestMapping(value= "/{locationId}", method = RequestMethod.GET)
    public ResponseEntity<LocationResource> fetch(@PathVariable("locationId") long locationdId) {

        LocationResource location = locationDao.findById(locationdId);

        return new ResponseEntity<>(location, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<?> list(@RequestParam Map<String, String> queryParams) {
        UserResource user = new UserResource();

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<LocationResource> q = cb.createQuery(LocationResource.class);
        Root<LocationResource> root = q.from(LocationResource.class);
        q.select(root);

        if(queryParams.containsKey("topLocations")) {

            q.orderBy(cb.desc(root.get("votes")));
        }

        List<LocationResource> locationList = em.createQuery(q).getResultList();

        if(queryParams.containsKey("nearbyLocations")) {
            float latitude = Float.parseFloat(queryParams.get("latitude"));
            float longitude = Float.parseFloat(queryParams.get("longitude"));
            float radius = Float.parseFloat(queryParams.get("radius"));

            for (Iterator<LocationResource> iter = locationList.listIterator(); iter.hasNext(); ) {
                LocationResource location = iter.next();
                if (Utils.distance(location.latitude, latitude, location.longitude, longitude, 0, 0) > radius) {
                    iter.remove();
                }
            }
        }

        if(queryParams.containsKey("search")) {
            String searchTerm = queryParams.get("search");
            int searchCount = 1;

            if(queryParams.containsKey("search")) {
                searchCount = Integer.parseInt(queryParams.get("searchCount"));
            }

            locationList =
                    locationList
                    .stream()
                    .filter(l -> searchTerm.isEmpty() ? true: l.name.startsWith(searchTerm))
                    .sorted((l1, l2) -> l1.name.compareTo(l2.name))
                    .limit(searchCount)
                    .collect(Collectors.toList());
        }

        return new ResponseEntity<>(locationList, HttpStatus.OK);
    }

    @RequestMapping(value= "/{id}/users", method = RequestMethod.GET)
    public ResponseEntity<?> listLocation(@PathVariable("id") long locationId,
                                                               @RequestParam Map<String, String> queryParams) {
        HashMap<Long, UserResource> hm = new HashMap<>();

        List<UserLocationResources> userLocationResources = userLocationDao.findByLocationId(locationId);
        Collection<UserResource> users = new ArrayList<>();

        for(UserLocationResources userLocationResource: userLocationResources) {

            if(queryParams.containsKey("voted")) {
                if(userLocationResource.vote == 0) {
                    continue;
                }
            }

            UserResource user = userDao.findById(userLocationResource.userId);
            hm.put(user.id, user);
        }

        // Get unique users.
        users = hm.values();

        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createMultipartLocation(
            @CookieValue("Authorization") String accessToken,
            @RequestPart(value = "name", required = true) String name,
            @RequestPart(value = "description", required = true) String description,
            @RequestPart(value = "latitude", required = true) String latitude,
            @RequestPart(value = "longitude", required = true) String longitude,
            @RequestPart("file") MultipartFile image
            ) {

        LocationResource location = new LocationResource();
        location.latitude = Float.valueOf(latitude);
        location.longitude = Float.valueOf(longitude);
        location.name = name;
        location.description = description;
        location.createdBy = authorizationProvider.getUserContenxt(accessToken).id;

        if (!image.isEmpty()) {
            try {
                byte[] bytes = image.getBytes();
                location.image_url = "images/" + image.getOriginalFilename();

                BufferedOutputStream stream =
                        new BufferedOutputStream(new FileOutputStream(new File(appConfig.getStaticFileLocation() + "images/" + image.getOriginalFilename())));
                stream.write(bytes);
                stream.close();
                System.out.println("You successfully uploaded " + image.getOriginalFilename() + "!");
            } catch (Exception e) {
                System.out.println( "You failed to upload " + image.getOriginalFilename() + " => " + e.getMessage());
            }
        } else {
            location.image_url = "images/" + defaultImg;
        }

        try {

            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            System.out.println("Location: " + ow.writeValueAsString(location));

        } catch (Exception ex) {

        }

        locationDao.save(location);

        return new ResponseEntity<>(HttpStatus.CREATED);
    }
}