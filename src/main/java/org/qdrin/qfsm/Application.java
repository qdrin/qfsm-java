package org.qdrin.qfsm;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;

import org.qdrin.qfsm.model.*;
import org.qdrin.qfsm.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

import com.zaxxer.hikari.HikariDataSource;


@Slf4j
@SpringBootApplication
@EntityScan
public class Application implements CommandLineRunner {

	@Autowired
	DataSource dataSource;

	@Autowired
	ProductRepository productRepository;

	@Autowired
	FsmApp fsmApp;

	void saveProduct(Product product) {
		log.debug("saveProduct product: {}, prices: {}", product.getProductId(), product.getProductPrices());
		productRepository.save(product);
	}

	Optional<Product> getProduct(String productId) {
		Optional<Product> product = productRepository.findById(productId);
		return product;
	}

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
			Product product = (Product) fsmApp.getVariables().get("product");
			product.setProductId(mid);
			saveProduct((Product) fsmApp.getVariables().get("product"));
		}
		log.info("exiting...");
		in.close();
	}
}
