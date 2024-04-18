package org.qdrin.qfsm.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Value;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorModel {
    String serviceName;
    String errorCode;
    String userMessage;
    String developerMessage;
}
