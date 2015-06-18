package amt2fhir.util;

import java.util.logging.Logger;

public class LoggingTimer implements AutoCloseable {

    long startTime;
    Logger logger;
    String message;

    public LoggingTimer(Logger logger, String message) {
        this.startTime = System.currentTimeMillis();
        this.logger = logger;
        this.message = message;
        
        logger.info("Start timing: " + message);
    }

    @Override
    public void close() {
        logger.info("End timing: " + message + " - completed in " + (System.currentTimeMillis() - startTime) + "ms");
    }

}
