package org.qdrin.qfsm;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan
public class Application {  // implements CommandLineRunner {

	@Autowired
	DataSource dataSource;

	@Autowired
	FsmApp fsmApp;

	// static public final Scanner scanner = new Scanner(System.in);

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
