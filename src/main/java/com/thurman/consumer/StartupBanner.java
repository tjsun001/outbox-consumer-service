package com.thurman.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupBanner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(StartupBanner.class);

    @Override
    public void run(ApplicationArguments args) {
        log.info("STARTUP_CHECK: consumer build id = 2026-01-20-1");
    }
}
