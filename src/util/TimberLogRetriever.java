package com.amazon.aws.vpn.telemetry.horizonte.webapp.util;


import amazon.platform.config.AppConfig;
import amazon.timber.TimberClientFactory;
import amazon.timber.TimberLogSummary;
import com.amazonaws.rip.RIPHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * Timber Log Retriever.
 * Note now it can be only used for PDX realm
 */
public class TimberLogRetriever {
    private static final Logger LOGGER = LogManager.getLogger(TimberLogRetriever.class);

    private static final String OWNER_EMAIL = AppConfig.findString("AwsVpnTelemetryHorizonte.timber_owner_email");
    private static final String GROUP_NAME = AppConfig.findString("AwsVpnTelemetryHorizonte.timber_group_name");
    private static final String MATERIAL_SET = AppConfig.findString("AwsVpnTelemetryHorizonte.timber_material_set");
    private static final String ENDPOINT = AppConfig.findString("AwsVpnTelemetryHorizonte.timber_endpoint");
    private static final String LOG_NAME = "%";
    private static final String REGION = RIPHelper.local().region(AppConfig.getRealm().name()).regionName();
    private static final String OWNER = AppConfig.findString("AwsVpnTelemetryHorizonte.timber_account");
    private static final int RETRIES = 5;
    private static final int ONE_HOUR = 3600000;
    private final TimberClientFactory factory;

    /**
     * Constructor.
     */
    public TimberLogRetriever() {
        LOGGER.info("Initialize the timber log retriever");
        this.factory = new TimberClientFactory(MATERIAL_SET) {
            {
                setEndpoint(ENDPOINT);
                setMaxRetries(RETRIES);
                setRegion(REGION);
            }
        };
        LOGGER.info("Initialization Done.");
    }

    /**
     * Get Logs as a map output.
     *
     * @param startDate     the start time of Log
     * @param searchableKey the searchable key {cgw-ip : XXXX ; vgw-ip : XXXX}
     * @return the arraylist of map outputs of 1 hour log since the start time
     * @throws RuntimeException BufferedReader cannot closed
     */
    public ArrayList<Map<String, String>> getLogs(Date startDate, Map<String, String> searchableKey) throws RuntimeException {
        LOGGER.info("Starting getting Logs");
        ArrayList<Map<String, String>> logsList = new ArrayList<>();
        List<TimberLogSummary> timberLogSummaries = getTimberLogSummaries(startDate, searchableKey);
        int count = 0;
        LOGGER.info("Starting parser.");
        for (TimberLogSummary timberLogSummary : timberLogSummaries) {
            count++;
            LOGGER.info("Getting Log " + count);
            Map<String, String> logs = new HashMap<>();
            logs.put("CGW IP", timberLogSummary.getSearchableKeys().get("cgw-ip"));
            logs.put("VGW IP", timberLogSummary.getSearchableKeys().get("vgw-ip"));
            logs.put("Start Time", timberLogSummary.getStartTime());
            logs.put("End Time", timberLogSummary.getEndTime());
            logs.put("Log Name", timberLogSummary.getLogName());
            logs.put("Log ID", timberLogSummary.getLogId());
            logs.put("Size KB", Double.toString(timberLogSummary.getSize() / 1024.0));

            BufferedReader reader = null;
            InputStream is = factory.getInstance().getGZLogStream(timberLogSummary.getLogId());
            LOGGER.info("inputStream is initialized");
            InputStreamReader isReader = new InputStreamReader(is, StandardCharsets.UTF_8);
            reader = new BufferedReader(isReader);
            LOGGER.info("Got Buffered Reader");
            logs.put("Log Report", reader.lines().collect(Collectors.joining("\n")));
            try {
                reader.close();
            } catch (IOException e) {
                LOGGER.error("BufferedReader cannot be closed");
                throw new RuntimeException(e);
            }
            logsList.add(logs);
            LOGGER.info("Log " + count + " done");
        }
        return logsList;
    }

    private List<TimberLogSummary> getTimberLogSummaries(Date startDate,
                                                         Map<String, String> searchableKey) {
        //searchableKey = {cgw-ip : XXXX ; vgw-ip : XXXX}
        LOGGER.info("Getting Timber Log Summaries started from "
                + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(startDate)
                + " with cgw-ip " + searchableKey.get("cgw-ip")
                + " and vgw-ip " + searchableKey.get("vgw-ip"));
        Date endDate = (new Date(startDate.getTime() + ONE_HOUR + 1));
        LOGGER.info("Time interval is " + (endDate.getTime() - startDate.getTime()));
        List<TimberLogSummary> timberLogSummaries = factory.listLogs()
                .withOwnerEmail(OWNER_EMAIL)
                .withOwner(OWNER)
                .withLogGroupName(GROUP_NAME)
                .withLogName(LOG_NAME)
                .withSearchableKeys(searchableKey)
                .withStartTime(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(startDate))
                .withEndTime(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(endDate))
                .call();
        LOGGER.info("GOT Timber Log Summaries.");
        return timberLogSummaries;
    }

}
