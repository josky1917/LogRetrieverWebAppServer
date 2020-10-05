package com.amazon.aws.vpn.telemetry.horizonte.webapp.controller.core.CoralClient;

import com.amazon.aws.vpn.telemetry.horizonte.webapp.response.ClientResponse;
import com.amazon.aws.vpn.telemetry.horizonte.webapp.util.Lighthouse;
import com.amazon.aws.vpn.telemetry.horizonte.webapp.util.User;
import com.amazon.horizonte.spring.annotations.PageType;
import com.amazon.horizonte.spring.stereotype.HorizonteController;
import com.amazon.vpnlighthouse.RemoveCustomerLimitRequest;
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
 * This is a Remove Customer Limit Controller.
 */
@Controller
@HorizonteController
public class RemoveCustomerLimitController {
    private static final Logger LOGGER = LogManager.getLogger(RemoveCustomerLimitController.class);

    /**
     * Remove Customer Limit API call controller method.
     *
     * @param request request from react client
     * @return Case Update Response
     */
    @RequestMapping(value = "/RemoveCustomerLimitRequestCall", method = RequestMethod.POST)
    @PageType(pageType = "RemoveCustomerLimit")
    @ResponseBody
    public ClientResponse removeCustomerLimitAPICall(HttpServletRequest request) {
        LOGGER.info("Executing Remove Customer Limit api.");
        String userID = (new User()).getUserId(request);
        String accountId;
        String propertyName;
        try {
            LOGGER.info("Receiving request from react client. User ID : " + userID);
            String requestBody = IOUtils.toString(request.getReader());
            JSONObject requestBodyObject = new JSONObject(requestBody);
            accountId = requestBodyObject.getJSONObject("requestData").getString("accountId");
            propertyName = requestBodyObject.getJSONObject("requestData").getString("propertyName");
            LOGGER.info("Request received.");
        } catch (IOException | JSONException e) {
            LOGGER.error("Request validation failed. " + e.toString(), e);
            return new ClientResponse(
                    "Request is not valid", new Exception("Invalid request for single tunnel notification operation"), userID);
        }

        try {
            LOGGER.info(userID + " is removing customer limit for " + accountId
                    + " with property name " + propertyName);
            Lighthouse mLighthouse = new Lighthouse();
            VpnLightHouseServiceClient client = mLighthouse.getVpnLightHouseClient();
            client.newRemoveCustomerLimitCall().call(RemoveCustomerLimitRequest.builder()
                    .withOwnerAccount(accountId)
                    .withPropertyName(propertyName)
                    .build());
            LOGGER.info("Successfully removed customer limit for " + accountId
                    + " with property name " + propertyName);
            return new ClientResponse("Successfully removed customer limit for " + accountId
                    + " with property name " + propertyName, userID);
        } catch (Exception e) {
            LOGGER.error("Request declined. " + e.toString(), e);
            return new ClientResponse("LightHouse Request Decline\n" + e.toString(), e, userID);
        }
    }
}
