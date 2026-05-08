package com.medinote.medinotebackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MediNoteApplication {

    public static void main(String[] args) {
        SpringApplication.run(MediNoteApplication.class, args);
    }

}
