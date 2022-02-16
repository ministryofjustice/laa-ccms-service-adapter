package uk.gov.justice.laa.ccms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.WebApplicationInitializer;

@SpringBootApplication
@ComponentScan(basePackages = {"com.oracle.determinations.server", "uk.gov.justice.laa.ccms"})
public class SpringCxfApplication  {

  public static void main(String[] args) {
    SpringApplication.run(SpringCxfApplication.class, args);
  }
}
