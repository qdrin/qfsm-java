package org.qdrin.qfsm.model.entity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import org.hibernate.Hibernate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.qdrin.qfsm.model.ProductPrice;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "products")
// @Data
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class ProductEntity {
  @Id private String productId;
  private String state;
  private String partyRoleId;
  private String productOfferingId;
  private String ProductOfferingName;
  private String ProductOfferingVersion;
  private String ProductSpecificationId;
  private String ProductSpecificationVersion;
  private boolean isBundle;
  private String status;
  private int productClass;
  private int tarificationPeriod;
  @JdbcTypeCode(SqlTypes.JSON)
  private List<ProductPrice> productPrices;
  private OffsetDateTime trialEndDate;
  private OffsetDateTime activeEndDate;
  private OffsetDateTime productStartDate;
//   @JdbcTypeCode(SqlTypes.JSON)
//   private List<JsonNode> productRelationships;
//   @JdbcTypeCode(SqlTypes.JSON)
//   private List<JsonNode> fabricRefs;
//   @JdbcTypeCode(SqlTypes.JSON)
//   private List<JsonNode> characteristics;
//   @JdbcTypeCode(SqlTypes.JSON)
//   private List<JsonNode> labels;
//   @JdbcTypeCode(SqlTypes.JSON)
//   private JsonNode metaInfo;
//   @JdbcTypeCode(SqlTypes.JSON)
//   private JsonNode quantity;
//   @JdbcTypeCode(SqlTypes.JSON)
//   private JsonNode extraParams;
}
