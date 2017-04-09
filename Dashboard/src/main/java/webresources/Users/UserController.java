package webresources.Users;

// Imports ...

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserDao userDao;
    private final LocationDao locationDao;
    private final UserLocationDao userLocationDao;
    private final AuthorizationProvider authorizationProvider;

    @Autowired
    public UserController(UserDao userDao,
                          LocationDao locationDao,
                          UserLocationDao userLocationDao,
                          AuthorizationProvider authorizationProvider) {
        this.userDao = userDao;
        this.locationDao = locationDao;
        this.userLocationDao = userLocationDao;
        this.authorizationProvider = authorizationProvider;
    }

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<UserResource> create(@RequestBody UserResource receivedUser,
                                               HttpServletRequest request) {

        receivedUser = userDao.save(receivedUser);

        HttpHeaders headers = new HttpHeaders();

        headers.add("Location", request.getServletPath() + "/" + receivedUser.id);
        headers.add("Authorization", authorizationProvider.generateAccessToken(receivedUser));
        return new ResponseEntity<>(headers, HttpStatus.CREATED);
    }

    // Post is not idempotent so we can use the user/login resource endpoint.
    // Post is acceptable as a state transition method http://stackoverflow.com/questions/10279365/custom-action-in-restful-service.
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public ResponseEntity<?> login(@RequestBody UserResource receivedUser) {

        UserResource user = userDao.findByEmailAndPassword(receivedUser.email,
                receivedUser.getPassword());

        if(user == null) {
            return new ResponseEntity<Void>(HttpStatus.UNAUTHORIZED);
        }

        String accessToken = authorizationProvider.generateAccessToken(user);

        JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
        ObjectNode response = nodeFactory.objectNode();
        response.put("access_token", accessToken);
        response.put("userId", user.id);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<?> list(@RequestParam Map<String, String> queryParams) {
        UserResource user = new UserResource();

        if(queryParams.containsKey("exists")) {
            UserResource userExists = userDao.findByEmail(queryParams.get("exists"));

            if(userExists != null) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

        }

        List<UserResource> userList = userDao.findAll();

        return new ResponseEntity<>(userList, HttpStatus.OK);
    }

    @RequestMapping(value= "/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> fetch(@PathVariable("id") long userId) {
        UserResource user = userDao.findById(userId);

        if(user == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @RequestMapping(value= "/{id}/locations", method = RequestMethod.GET)
    public ResponseEntity<List<LocationResource>> listLocation(@PathVariable("id") long userId) {
        List<UserLocationResources> userLocations = userLocationDao.findByUserId(userId);
        List<LocationResource> locations = new ArrayList<>();

        for(UserLocationResources userLocation: userLocations) {
            LocationResource location = locationDao.findById(userLocation.locationId);
            locations.add(location);
        }

        return new ResponseEntity<>(locations, HttpStatus.OK);
    }

    @RequestMapping(value= "/{id}/locations", method = RequestMethod.POST)
    public ResponseEntity<?> addLocation(@PathVariable("id") long userId,
                                         @RequestBody ObjectNode obj) {

        UserLocationResources userLocation = new UserLocationResources();
        userLocation.locationId = obj.get("id").asInt();
        userLocation.userId = userId;

        userLocationDao.save(userLocation);

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @RequestMapping(value= "/{userId}/locations/{locationId}/{vote}", method = RequestMethod.POST)
    public ResponseEntity<?> vote(@PathVariable("userId") long userId,
                                  @PathVariable("locationId") long locationId,
                                  @PathVariable("vote") String vote) {

        LocationResource location = locationDao.findById(locationId);
        UserLocationResources userLocation = userLocationDao.findByUserIdAndLocationId(userId, locationId);

        if(userLocation == null) {
            userLocation = new UserLocationResources();
            userLocation.locationId = locationId;
            userLocation.userId = userId;
        }

        switch(vote) {
            case "upvote":
                if(userLocation.vote + 1 <= 1) { userLocation.vote += 1; location.votes += 1; }
                break;
            case "downvote":
                if(userLocation.vote -1 >= -1) { userLocation.vote -= 1;location.votes -=1; }
                break;
            default:
                userLocation.vote = 0;
        }


        locationDao.save(location);
        userLocationDao.save(userLocation);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}