package org.qdrin.qfsm;

import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

import com.zaxxer.hikari.HikariDataSource;


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

	// @Override
	// public void run(String... args) throws Exception {
	// 	HikariDataSource hds = (HikariDataSource) dataSource;
	// 	log.info("database: {}", hds.getJdbcUrl());
	// 	String mid = "m1";
	// 	while(! mid.equals("exit")) {
	// 		System.out.print("input machineId:");
	// 		mid = scanner.nextLine();
	// 		if(mid.isEmpty()) {continue;}
	// 		fsmApp.sendUserEvent(mid);
	// 	}
	// 	scanner.close();
	// 	log.info("exiting...");
	// }
}
