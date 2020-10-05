package com.amazon.aws.vpn.telemetry.horizonte.webapp.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.Map;

/**
 * Historical Logs Response.
 * logsList: a List of Logs information
 * exception: error massage
 * requesterID: Current user ID
 */
@Data
public class HistoricalLogResponse {
    private static final String DEFAULT_ERROR_MSG = "Unexpected exception occurred. Please contact woodchipper team for help.";

    private final ArrayList<Map<String, String>> logsList;
    private final String exception;
    private final String requesterID;

    /**
     * Error occurred.
     *
     * @param e           exception
     * @param requesterID requesterID
     */
    public HistoricalLogResponse(Exception e, String requesterID) {
        this(null, e, requesterID);
    }

    /**
     * Succeed.
     *
     * @param logsList    logsList
     * @param requesterID requesterID
     */
    public HistoricalLogResponse(ArrayList<Map<String, String>> logsList, String requesterID) {
        this(logsList, null, requesterID);
    }

    /**
     * main constructor.
     *
     * @param logsList    logsList
     * @param e           exception
     * @param requesterID requesterID
     */
    public HistoricalLogResponse(ArrayList<Map<String, String>> logsList, Exception e, String requesterID) {
        this.logsList = logsList;
        this.exception = e != null
                ? e.toString()
                : "";
        this.requesterID = requesterID;
    }
}
