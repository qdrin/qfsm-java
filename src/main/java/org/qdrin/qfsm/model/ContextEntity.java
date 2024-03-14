package org.qdrin.qfsm.model;

import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "contexts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContextEntity {
  @Id String productId;
  // @Id String machineId;
  @Lob
  byte[] context;
}
