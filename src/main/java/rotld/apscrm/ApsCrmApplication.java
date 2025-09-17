package rotld.apscrm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ApsCrmApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApsCrmApplication.class, args);
    }

}
