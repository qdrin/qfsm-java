package org.qdrin.qfsm;

import java.util.Scanner;

import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.zaxxer.hikari.HikariDataSource;


@Slf4j
@SpringBootApplication
public class Application implements CommandLineRunner {

	@Autowired
	DataSource dataSource;

	@Autowired
	FsmApp fsmApp;

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		HikariDataSource hds = (HikariDataSource) dataSource;
		log.info("database: {}", hds.getJdbcUrl());
		Scanner in = new Scanner(System.in);
		String mid = "m1";
		while(! mid.equals("exit")) {
			System.out.print("input machineId:");
			mid = in.nextLine();
			fsmApp.sendUserEvent(mid, in);
		}
		log.info("exiting...");
		in.close();
	}
}
