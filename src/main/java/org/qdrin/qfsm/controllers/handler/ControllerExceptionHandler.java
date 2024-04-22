package org.qdrin.qfsm.controllers.handler;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import org.qdrin.qfsm.model.dto.ErrorModel;

@RestControllerAdvice
@Slf4j
public class ControllerExceptionHandler  {
    private static HashMap<String, HttpStatus> statuses = new HashMap<String, HttpStatus>() {{
        put("UnknownError", HttpStatus.INTERNAL_SERVER_ERROR);
        put("NotAcceptedEventException", HttpStatus.BAD_REQUEST);
        put("RepeatedEventException", HttpStatus.BAD_REQUEST);
        put("BadUserDataException", HttpStatus.BAD_REQUEST);
    }};

    @Value(value="${server.servlet.context-path}")
    String basePath;

    @ExceptionHandler({
            Exception.class
    })
    public ResponseEntity<Object> handleOthersException(Exception ex, WebRequest request) {
        log.error("Cause: {}, message: {}", ex.getCause(), ex.getMessage());
        String name = ex.getClass().getSimpleName();
        var message = ex.getLocalizedMessage();
        log.error("exception: {}, message: {}", name, message);
        HttpStatus status = statuses.getOrDefault(name, HttpStatus.INTERNAL_SERVER_ERROR);
        ResponseEntity<Object> resp = ResponseEntity.status(status).body(new ErrorModel(basePath, name, message, ex.toString()));
        return resp;
    }



}
