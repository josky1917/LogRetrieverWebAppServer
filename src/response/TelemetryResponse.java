package com.amazon.aws.vpn.telemetry.horizonte.webapp.response;

import lombok.Data;

import java.util.Map;

/**
 * Telemetry Response.
 * wrappedData: nested Map of telemetry info from endpoint.
 * exception: error massage.
 * requesterID: Current user ID.
 */
@Data
public class TelemetryResponse {
    private static final String DEFAULT_ERROR_MSG = "Unexpected exception occurred. Please contact woodchipper team for help.";

    private final Map<String, Object> wrappedData;
    private final String exception;
    private final String requesterID;

    /**
     * Error occurred.
     *
     * @param e           exception
     * @param requesterID requesterID
     */
    public TelemetryResponse(Exception e, String requesterID) {
        this(null, e, requesterID);
    }

    /**
     * Succeed.
     *
     * @param wrappedData wrappedData
     * @param requesterID requesterID
     */
    public TelemetryResponse(Map<String, Object> wrappedData, String requesterID) {
        this(wrappedData, null, requesterID);
    }

    /**
     * main constructor.
     *
     * @param wrappedData wrappedData
     * @param e           exception
     * @param requesterID requesterID
     */
    public TelemetryResponse(Map<String, Object> wrappedData, Exception e, String requesterID) {
        this.wrappedData = wrappedData;
        this.exception = e != null
                ? e.toString()
                : "";
        this.requesterID = requesterID;
    }
}
