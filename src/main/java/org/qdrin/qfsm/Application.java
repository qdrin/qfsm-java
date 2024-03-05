package org.qdrin.qfsm;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;

import org.qdrin.qfsm.model.*;
import org.qdrin.qfsm.model.entity.ProductEntity;
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
		// Product product = new Product();
		ProductEntity pe = new ProductEntity();
		// System.out.print("input productId:");
		// product.setProductId(in.nextLine());
		// System.out.print("input productOfferingId:");
		// product.setProductOfferingId(in.nextLine());
		// ProductPrice price = new ProductPrice();
		// List<ProductPrice> prices = new ArrayList<>();
		// System.out.print("input price priceId:");
		// price.setPriceId(in.nextLine());
		// System.out.print("input price productStatus:");
		// price.setProductStatus(in.nextLine());
		// prices.add(price);
		// product.setProductPrices(prices);

		pe.setProductId(product.getProductId());
		pe.setProductOfferingId(product.getProductOfferingId());
		pe.setProductPrices(product.getProductPrices());
		log.debug("saveProduct product: {}", product);
		log.debug("saveProduct productEntity: {}", pe);
		productRepository.save(pe);
	}

	Optional<ProductEntity> getProduct(String productId) {
		Optional<ProductEntity> pe = productRepository.findById(productId);
		return pe;
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
