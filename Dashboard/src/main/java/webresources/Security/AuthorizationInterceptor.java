package webresources.Security;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import webresources.Users.UserDao;
import webresources.Users.UserResource;

@Component
public class AuthorizationInterceptor implements HandlerInterceptor  {

    private final UserDao userDao;
    private final AuthorizationProvider authorizationProvider;

    @Autowired
    public AuthorizationInterceptor(UserDao userDao, AuthorizationProvider authorizationProvider) {
        this.userDao = userDao;
        this.authorizationProvider = authorizationProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {



      if ((request.getServletPath().equals("/users") || request.getServletPath().equals("/users/login"))
                && request.getMethod() == "POST") {
            return true;
        }

        if ((request.getServletPath().equals("/users") && request.getQueryString().startsWith("exists")
                && request.getMethod() == "GET")) {
            return true;
        }

        String authorization = request.getHeader("Authorization");

        if(authorization == null) {
            if(request.getCookies() != null) {
                for (Cookie c : request.getCookies()) {
                    if (c.getName().equals("Authorization")) {
                        authorization = c.getValue();
                        break;
                    }
                }
            }
        }

        if(authorization != null) {
            UserResource user = authorizationProvider.getUserContenxt(authorization);

            if(user == null) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                return false;
            } else {
                return true;
            }

        } else
        {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return false;
        }
    }
    @Override
    public void postHandle(	HttpServletRequest request, HttpServletResponse response,
                               Object handler, ModelAndView modelAndView) throws Exception {
        System.out.println("---method executed---");
    }
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) throws Exception {
        System.out.println("---Request Completed---");
    }
} 