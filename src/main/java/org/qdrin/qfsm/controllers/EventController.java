package org.qdrin.qfsm.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;


import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
@Service
@Slf4j
public class EventController {
    private final ProcessService processService;
    
    @Autowired
    DataSource dataSource;

    @PostMapping("/sync")
    public ResponseEntity<ResponseDto> startSyncProcessInstance(@RequestBody @Valid RequestDto syncDto) {
        HikariPoolMXBean pool = ((HikariDataSource) dataSource).getHikariPoolMXBean();
        log.debug("POST sync started. orderId: {}. db connections total: {}, active: {}, idle: {}, waiting: {}",
            syncDto.getOrderId(),
            pool.getTotalConnections(),
            pool.getActiveConnections(),
            pool.getIdleConnections(),
            pool.getThreadsAwaitingConnection());
        var responseModel = processService.startSyncProcess(syncDto);

        var response = ResponseEntity.ok(responseModel);

        log.debug("POST sync startSyncProcess finished orderId: {}. db connections total: {}, active: {}, idle: {}, waiting: {}",
            syncDto.getOrderId(),
            pool.getTotalConnections(),
            pool.getActiveConnections(),
            pool.getIdleConnections(),
            pool.getThreadsAwaitingConnection());
        log.info("POST /process/start/sync finished: {}", response);
        return ResponseEntity.ok(responseModel);
    }

    @PostMapping("/async")
    public ResponseEntity<ResponseDto> startAsyncProcessInstance(@RequestBody @Valid RequestDto syncDto) {
        var responseModel = processService.startAsyncProcess(syncDto);
        var response = ResponseEntity.ok(responseModel);
        log.info("POST /process/start/async finished: {}", response);
        return ResponseEntity.ok(responseModel);
    }
}

