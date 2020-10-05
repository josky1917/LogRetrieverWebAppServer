package com.amazon.aws.vpn.telemetry.horizonte.webapp.controller.core.CoralClient;

import com.amazon.aws.vpn.telemetry.horizonte.webapp.response.ClientResponse;
import com.amazon.aws.vpn.telemetry.horizonte.webapp.util.Lighthouse;
import com.amazon.aws.vpn.telemetry.horizonte.webapp.util.User;
import com.amazon.horizonte.spring.annotations.PageType;
import com.amazon.horizonte.spring.stereotype.HorizonteController;
import com.amazon.vpnlighthouse.RemoveCustomerLimitRequest;
import com.amazon.vpnlighthouse.UpdateCustomerLimitRequest;
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
 * This is a Single Tunnel Notification Controller.
 */
@Controller
@HorizonteController
public class SingleTunnelNotificationsController {
    private static final Logger LOGGER = LogManager.getLogger(SingleTunnelNotificationsController.class);

    /**
     * Enable/Disable single tunnel notification API call controller method.
     *
     * @param request request from react client
     * @return Case Update Response
     */
    @RequestMapping(value = "/NotificationsRequestCall", method = RequestMethod.POST)
    @PageType(pageType = "SingleTunnelNotifications")
    @ResponseBody
    public ClientResponse singleTunnelNotificationsAPICall(HttpServletRequest request) {
        LOGGER.info("Executing enable/disable single tunnel notification api.");
        String successMsg = "";
        String userID = (new User()).getUserId(request);
        String accountId = "";
        String tunnelOpt;
        try {
            LOGGER.info("Receiving request from react client. User ID : " + userID);
            String requestBody = IOUtils.toString(request.getReader());
            JSONObject requestBodyObject = new JSONObject(requestBody);
            accountId = requestBodyObject.getJSONObject("requestData").getString("accountId");
            tunnelOpt = requestBodyObject.getJSONObject("requestData").getString("tunnelOpt");
            LOGGER.info("Request received.");
        } catch (IOException | JSONException e) {
            LOGGER.error("Request validation failed. " + e.toString(), e);
            return new ClientResponse(
                    "Request is not valid", new Exception("Invalid request for single tunnel notification operation"), userID);
        }

        try {
            LOGGER.info("Requesting VPN lighthouse.");
            Lighthouse mLighthouse = new Lighthouse();
            VpnLightHouseServiceClient client = mLighthouse.getVpnLightHouseClient();
            if (tunnelOpt.equals("Disable")) {
                LOGGER.info(userID + " is disabling single tunnel notification for " + accountId);
                client.newUpdateCustomerLimitCall().call(UpdateCustomerLimitRequest.builder()
                        .withOwnerAccount(accountId)
                        .withPropertyName("Woodchipper.SingleTunnelNotifications.enabled")
                        .withPropertyValue("false")
                        .withPropertyComment("Disabling Silvermine Notifications")
                        .build());
                LOGGER.info("Account " + accountId + " will no longer receive silvermine notifications.");
                successMsg = "Account " + accountId + " will no longer receive silvermine notifications";
            } else {
                LOGGER.info(userID + " is enabling single tunnel notification for " + accountId);
                client.newRemoveCustomerLimitCall().call(RemoveCustomerLimitRequest.builder()
                        .withOwnerAccount(accountId)
                        .withPropertyName("Woodchipper.SingleTunnelNotifications.enabled")
                        .build());
                LOGGER.info("Account " + accountId + " will receive silvermine notifications");
                successMsg = "Account " + accountId + " will receive silvermine notifications";
            }
        } catch (Exception e) {
            LOGGER.error("Request declined. " + e.toString(), e);
            return new ClientResponse("LightHouse Request Decline\n" + e.toString(), e, userID);
        }

        return new ClientResponse(successMsg, userID);
    }
}
