package com.amazon.aws.vpn.telemetry.horizonte.webapp.response;

import lombok.Data;

/**
 * CoralService Client Response (used for Lighthouse & Asterix service client).
 * successMsg: prompt that user will received on web page.
 * exception: error massage.
 * requesterID: Current user ID.
 */
@Data
public class ClientResponse {
    private static final String DEFAULT_ERROR_MSG = "Unexpected exception occurred. Please contact woodchipper team for help.";

    private final String successMsg;
    private final String exception;
    private final String requesterID;

    /**
     * Error occurred.
     *
     * @param e           exception
     * @param requesterID requesterID
     */
    public ClientResponse(Exception e, String requesterID) {
        this("", e, requesterID);
    }

    /**
     * Succeed.
     *
     * @param successMsg  successMsg
     * @param requesterID requesterID
     */
    public ClientResponse(String successMsg, String requesterID) {
        this(successMsg, null, requesterID);
    }

    /**
     * main constructor.
     *
     * @param successMsg  successMsg
     * @param e           exception
     * @param requesterID requesterID
     */
    public ClientResponse(String successMsg, Exception e, String requesterID) {
        this.successMsg = successMsg;
        this.exception = e != null
                ? DEFAULT_ERROR_MSG
                : "";
        this.requesterID = requesterID;
    }
}
