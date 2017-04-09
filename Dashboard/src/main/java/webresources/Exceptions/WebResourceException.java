package webresources.Exceptions;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by unknown_user on 12/19/2015.
 */
public class WebResourceException extends RuntimeException {
    public int error_code;
    public String msg;
}
