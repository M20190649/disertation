package webresources;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created by unknown_user on 1/10/2016.
 */

// Be aware!!! This class requires explicit getters and setters.
@ConfigurationProperties(ignoreUnknownFields = false, prefix = "app")
public class ApplicationConfiguration {
    private String staticFileLocation;

    public void setStaticFileLocation(String staticFileLocation) {
        this.staticFileLocation = staticFileLocation;
    }

    public String getStaticFileLocation() {
        return this.staticFileLocation;
    }
}
