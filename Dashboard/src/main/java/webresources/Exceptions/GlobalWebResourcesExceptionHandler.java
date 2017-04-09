package webresources.Exceptions;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import webresources.Exceptions.WebResourceException;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by unknown_user on 12/19/2015.
 */
@ControllerAdvice
public class GlobalWebResourcesExceptionHandler {

    @ExceptionHandler
    public ResponseEntity<ObjectNode> handleException(HttpServletRequest req, Exception e) throws Exception {
        if (!(e instanceof WebResourceException)) {
            throw e;
        }

        WebResourceException ex = (WebResourceException)e;

        JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
        ObjectNode exception = nodeFactory.objectNode();
        exception.put("error_code", ex.error_code);
        exception.put("msg",ex.msg);

        return new ResponseEntity<ObjectNode>(exception, HttpStatus.BAD_REQUEST);
    }
}
