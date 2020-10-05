package com.amazon.aws.vpn.telemetry.horizonte.webapp.controller.core.CoralClient;

import com.amazon.aws.vpn.telemetry.horizonte.webapp.response.ClientResponse;
import com.amazon.aws.vpn.telemetry.horizonte.webapp.util.Lighthouse;
import com.amazon.aws.vpn.telemetry.horizonte.webapp.util.User;
import com.amazon.horizonte.spring.annotations.PageType;
import com.amazon.horizonte.spring.stereotype.HorizonteController;
import com.amazon.vpnlighthouse.RunCommandReplaceInstanceRequest;
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
 * This is a Replace Instance Controller.
 */
@Controller
@HorizonteController
public class ReplaceInstanceController {
    private static final Logger LOGGER = LogManager.getLogger(ReplaceInstanceController.class);

    /**
     * Replace Instance API call controller method.
     *
     * @param request request from react client
     * @return Case Update Response
     */
    @RequestMapping(value = "/ReplaceInstanceRequestCall", method = RequestMethod.POST)
    @PageType(pageType = "ReplaceInstance")
    @ResponseBody
    public ClientResponse replaceInstanceAPICall(HttpServletRequest request) {
        LOGGER.info("Executing replace instance api.");
        String userID = (new User()).getUserId(request);
        String vpnId;
        String managementIp;
        String comment;
        try {
            LOGGER.info("Receiving request from react client. User ID : " + userID);
            String requestBody = IOUtils.toString(request.getReader());
            JSONObject requestBodyObject = new JSONObject(requestBody);
            vpnId = requestBodyObject.getJSONObject("requestData").getString("vpnId");
            managementIp = requestBodyObject.getJSONObject("requestData").getString("managementIp");
            comment = requestBodyObject.getJSONObject("requestData").getString("comment");
            LOGGER.info("Request received.");
        } catch (IOException | JSONException e) {
            LOGGER.error("Request validation failed. " + e.toString(), e);
            return new ClientResponse(
                    "Request is not valid", new Exception("Invalid request for ReplaceInstance operation"), userID);
        }

        try {
            LOGGER.info(userID + " send a replace instance request to "
                    + vpnId + " and " + managementIp + " with comment " + comment);
            Lighthouse mLighthouse = new Lighthouse();
            VpnLightHouseServiceClient client = mLighthouse.getVpnLightHouseClient();
            SsmRunCommandResponse response = client.newRunCommandReplaceInstanceCall().call(RunCommandReplaceInstanceRequest.builder()
                    .withRequesterLogin(userID)
                    .withVpnId(vpnId)
                    .withManagementIp(managementIp)
                    .withComment(comment)
                    .build());
            LOGGER.info("Response received. S3 Bucket Name of replace instance request for "
                    + userID + " is " + response.getS3BucketName());
            return new ClientResponse(
                    "Successfully sent a replace instance request to Vpn ID " + vpnId
                            + " and " + managementIp
                            + "with comment " + comment
                            + ". S3 Bucket Name of replace instance request for "
                            + userID + " is " + response.getS3BucketName(),
                    userID);
        } catch (Exception e) {
            LOGGER.error("Request declined. " + e.toString(), e);
            return new ClientResponse(
                    "LightHouse Request Decline\n" + e.toString(),
                    e,
                    userID);
        }
    }
}
