package webresources.UsersLocations;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import javax.validation.constraints.NotNull;


@Entity
@Table(name = "users_locations")
public class UserLocationResources {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public long id;

    @JsonProperty("userId")
    @Column(name = "user_id")
    public long userId;

    @JsonProperty("locationId")
    @Column(name = "location_id")
    public long locationId;

    @JsonProperty("vote")
    @Column(nullable = true)
    public int vote;
}

