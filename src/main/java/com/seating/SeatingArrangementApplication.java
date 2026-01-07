package com.seating;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Main Application Class for Automatic Seating Arrangement System
 *
 * @author Seating System
 * @version 1.0.0
 */
@SpringBootApplication
@EnableJpaAuditing
public class SeatingArrangementApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeatingArrangementApplication.class, args);
    }
}
