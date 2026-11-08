package tn.govoyage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Classe principale : demarre l'application Spring Boot GO Voyage. */
@SpringBootApplication
public class GoVoyageApplication {

    /** Point d'entree de l'API. */
    public static void main(String[] args) {
        SpringApplication.run(GoVoyageApplication.class, args);
    }
}
