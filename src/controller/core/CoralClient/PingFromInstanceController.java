package com.amazon.aws.vpn.telemetry.horizonte.webapp.controller.core.CoralClient;

import com.amazon.aws.vpn.telemetry.horizonte.webapp.response.ClientResponse;
import com.amazon.aws.vpn.telemetry.horizonte.webapp.util.Lighthouse;
import com.amazon.aws.vpn.telemetry.horizonte.webapp.util.User;
import com.amazon.horizonte.spring.annotations.PageType;
import com.amazon.horizonte.spring.stereotype.HorizonteController;
import com.amazon.vpnlighthouse.RunCommandPingRequest;
import com.amazon.vpnlighthouse.SsmRunCommandResponse;
import com.amazon.vpnlighthouse.VpnLightHouseServiceClient;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * This is a Ping From Instance Controller.
 */
@Controller
@HorizonteController
public class PingFromInstanceController {
    private static final Logger LOGGER = LogManager.getLogger(PingFromInstanceController.class);

    /**
     * Ping from instance API call controller method.
     *
     * @param request request from react client
     * @return Case Update Response
     */
    @RequestMapping(value = "/PingFromInstanceRequestCall", method = RequestMethod.POST)
    @PageType(pageType = "PingFromInstance")
    @ResponseBody
    public ClientResponse pingFromInstanceAPICall(HttpServletRequest request) {
        LOGGER.info("Executing ping from instance api.");
        String userID = (new User()).getUserId(request);
        String vpnId;
        String managementIp;
        String destination;
        String sourceAddress;
        int count;
        String interval;
        try {
            LOGGER.info("Receiving request from react client. User ID: " + userID);
            String requestBody = IOUtils.toString(request.getReader());
            JSONObject requestBodyObject = new JSONObject(requestBody);
            vpnId = requestBodyObject.getJSONObject("requestData").getString("vpnId");
            managementIp = requestBodyObject.getJSONObject("requestData").getString("managementIp");
            destination = requestBodyObject.getJSONObject("requestData").getString("destination");
            sourceAddress = requestBodyObject.getJSONObject("requestData").getString("sourceAddress");
            count = requestBodyObject.getJSONObject("requestData").getInt("count");
            interval = requestBodyObject.getJSONObject("requestData").getString("interval");
            if (!interval.isEmpty()) {
                Double.parseDouble(interval);
            }
            LOGGER.info("Request received.");
        } catch (IOException | JSONException | NumberFormatException e) {
            LOGGER.error("Request validation failed. " + e.toString(), e);
            return new ClientResponse(
                    "Request is not valid", new Exception("Invalid request for PingFromInstance operation"), userID);
        }

        try {
            LOGGER.info(userID + "is sending a ping request to Vpn ID " + vpnId + " and " + managementIp
                    + " with destination " + destination + ", source address " + sourceAddress
                    + ", count " + count + ", interval " + interval);
            Lighthouse mLighthouse = new Lighthouse();
            VpnLightHouseServiceClient client = mLighthouse.getVpnLightHouseClient();
            SsmRunCommandResponse response = client.newRunCommandPingCall().call(RunCommandPingRequest.builder()
                    .withVpnId(vpnId)
                    .withManagementIp(managementIp)
                    .withDestination(destination)
                    .withSourceAddress(sourceAddress.isEmpty() ? null : sourceAddress)
                    .withCount(count)
                    .withInterval(interval.isEmpty() ? null : Double.parseDouble(interval))
                    .build());
            LOGGER.info("Response received. S3 Bucket Name of ping request for " + userID
                    + " is " + response.getS3BucketName());
            return new ClientResponse(
                    "Successfully sent a ping request to Vpn ID " + vpnId
                            + " and " + managementIp + " with destination " + destination
                            + ", source address " + sourceAddress
                            + ", count " + count
                            + ", interval " + interval
                            + ". S3 Bucket Name of ping request for " + userID
                            + " is " + response.getS3BucketName(),
                    userID);
        } catch (RuntimeException e) {
            LOGGER.error("Runtime exception caught." + e.toString(), e);
            return new ClientResponse(
                    "Runtime exception caught.", e, userID);
        } catch (Exception e) {
            LOGGER.error("Request declined. " + e.toString(), e);
            return new ClientResponse(
                    "LightHouse Request Decline\n" + e.toString(),
                    e,
                    userID);
        }
    }
}

