package com.animesh.pricetracker;

import com.animesh.pricetracker.service.NotificationService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PriceTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PriceTrackerApplication.class, args);
        ApplicationContext ctx = new AnnotationConfigApplicationContext(NotificationService.class);
        ctx.getBean("sendPriceAlert");
    }

}
