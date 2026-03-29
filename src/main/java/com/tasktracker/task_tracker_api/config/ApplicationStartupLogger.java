package com.tasktracker.task_tracker_api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ApplicationStartupLogger {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationStartupLogger.class);

    @Value("${server.port:8080}")
    private String serverPort;

    @EventListener(ApplicationReadyEvent.class)
    public void logApplicationReady() {
        logger.info("Task Tracker API started successfully and is ready to receive requests on port {}", serverPort);
    }
}
