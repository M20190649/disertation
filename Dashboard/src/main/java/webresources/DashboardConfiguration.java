package webresources;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.resource.GzipResourceResolver;
import org.springframework.web.servlet.resource.PathResourceResolver;
import webresources.Security.AuthorizationInterceptor;

/*
http://stackoverflow.com/questions/31082981/spring-boot-adding-http-request-interceptors
http://therealdanvega.com/blog/2015/10/23/spring-boot-application-annotation
http://www.concretepage.com/spring/spring-mvc/spring-handlerinterceptor-annotation-example-webmvcconfigureradapter
@SpringBootApplication automatically contains @EnableAutoConfiguration and @ComponentScan.
images is different from images/ :)
 */
@Configuration
@SpringBootApplication(exclude = {MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
@EnableConfigurationProperties(ApplicationConfiguration.class)
public class DashboardConfiguration extends WebMvcConfigurerAdapter {

    @Autowired
    AuthorizationInterceptor authorizationInterceptor;

    @Autowired
    ApplicationConfiguration appConfig;

    public static void main(String[] args) {
        SpringApplication.run(DashboardConfiguration.class, args);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(authorizationInterceptor);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
                .addResourceHandler("/**")
                .addResourceLocations("file:///" + appConfig.getStaticFileLocation())
                .resourceChain(false)
                .addResolver(new GzipResourceResolver())
                .addResolver(new PathResourceResolver());

        registry
                // ** matches any path inside images. *just matches the first level of the directory.
                .addResourceHandler("/images/**")
                .addResourceLocations("file:///" + appConfig.getStaticFileLocation() + "images/")
                    .resourceChain(false) // don't cache
                    .addResolver(new GzipResourceResolver())
                    .addResolver(new PathResourceResolver());
}

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedMethods("*"); // by default other flags are set.
    }
}