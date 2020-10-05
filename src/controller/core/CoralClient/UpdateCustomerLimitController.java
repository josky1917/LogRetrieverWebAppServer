package com.amazon.aws.vpn.telemetry.horizonte.webapp.controller.core.CoralClient;

import com.amazon.aws.vpn.telemetry.horizonte.webapp.response.ClientResponse;
import com.amazon.aws.vpn.telemetry.horizonte.webapp.util.Lighthouse;
import com.amazon.aws.vpn.telemetry.horizonte.webapp.util.User;
import com.amazon.horizonte.spring.annotations.PageType;
import com.amazon.horizonte.spring.stereotype.HorizonteController;
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
 * This is a Update Customer Limit Controller.
 */
@Controller
@HorizonteController
public class UpdateCustomerLimitController {
    private static final Logger LOGGER = LogManager.getLogger(UpdateCustomerLimitController.class);

    /**
     * Update Customer Limit controller method.
     *
     * @param request request from react client
     * @return Case Update Response
     */
    @RequestMapping(value = "/UpdateCustomerLimitRequestCall", method = RequestMethod.POST)
    @PageType(pageType = "UpdateCustomerLimit")
    @ResponseBody
    public ClientResponse updateCustomerLimitAPICall(HttpServletRequest request) {
        LOGGER.info("Executing Update Customer Limit api.");
        String userID = (new User()).getUserId(request);
        String accountId;
        String propertyName;
        String propertyValue;
        String comment;
        try {
            LOGGER.info("Receiving request from react client. User ID : " + userID);
            String requestBody = IOUtils.toString(request.getReader());
            JSONObject requestBodyObject = new JSONObject(requestBody);
            accountId = requestBodyObject.getJSONObject("requestData").getString("accountId");
            propertyName = requestBodyObject.getJSONObject("requestData").getString("propertyName");
            propertyValue = requestBodyObject.getJSONObject("requestData").getString("propertyValue");
            comment = requestBodyObject.getJSONObject("requestData").getString("comment");
            LOGGER.info("Request received.");
        } catch (IOException | JSONException e) {
            LOGGER.error("Request validation failed. " + e.toString(), e);
            return new ClientResponse(
                    "Request is not valid",
                    new Exception("Invalid request for PinCustomerToAmi operation"),
                    userID);
        }

        try {
            LOGGER.info(userID + " is updating customer limit for  " + accountId + " setting property name "
                    + propertyName + " to " + propertyValue + "with comment " + comment);
            Lighthouse mLighthouse = new Lighthouse();
            VpnLightHouseServiceClient client = mLighthouse.getVpnLightHouseClient();
            client.newUpdateCustomerLimitCall().call(UpdateCustomerLimitRequest.builder()
                    .withOwnerAccount(accountId)
                    .withPropertyName(propertyName)
                    .withPropertyValue(propertyValue)
                    .withPropertyComment(comment)
                    .build());
            LOGGER.info("Response received. Successfully updated customer limit for " + accountId
                    + " setting property name " + propertyName + " to " + propertyValue + " with comment " + comment);
            return new ClientResponse(
                    "Response received. Successfully updated customer limit for " + accountId
                            + " setting property name " + propertyName + " to "
                            + propertyValue + " with comment " + comment,
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
