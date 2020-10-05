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
 * This is a Pin Customer Account To Instance Size Controller.
 */
@Controller
@HorizonteController
public class PinCustomerAccountToInstanceSizeController {
    private static final Logger LOGGER = LogManager.getLogger(PinCustomerAccountToInstanceSizeController.class);

    /**
     * Pin Customer Account To Instance Size API call controller method.
     *
     * @param request request from react client
     * @return Case Update Response
     */
    @RequestMapping(value = "/PinCustomerAccountToInstanceSizeRequestCall", method = RequestMethod.POST)
    @PageType(pageType = "PinCustomerAccountToInstanceSize")
    @ResponseBody
    public ClientResponse replaceInstanceAPICall(HttpServletRequest request) {
        LOGGER.info("Executing pin customer account to instance size api.");
        String userID = (new User()).getUserId(request);
        String accountId;
        String instanceSize;
        String comment;
        try {
            LOGGER.info("Receiving request from react client. User ID : " + userID);
            String requestBody = IOUtils.toString(request.getReader());
            JSONObject requestBodyObject = new JSONObject(requestBody);
            accountId = requestBodyObject.getJSONObject("requestData").getString("accountId");
            instanceSize = requestBodyObject.getJSONObject("requestData").getString("instanceSize");
            comment = requestBodyObject.getJSONObject("requestData").getString("comment");
            LOGGER.info("Request received.");
            if (!instanceSize.equals("c5.large")) {
                LOGGER.info("Returning an error due to pinned instance size not matching c5.large");
                return new ClientResponse(
                        "Accounts may only be pinned to c5.large. "
                                + "Please contact the Woodchipper team if you believe this is incorrect.",
                        new Exception("Invalid request for PinCustomerAccountToInstanceSize operation"),
                        userID);
            }
        } catch (IOException | JSONException e) {
            LOGGER.error("Request validation failed. " + e.toString(), e);
            return new ClientResponse(
                    "Request is not valid",
                    new Exception("Invalid request for PinCustomerAccountToInstanceSize operation"),
                    userID);
        }

        try {
            LOGGER.info(userID + " is pinning account " + accountId + " to instance size "
                    + instanceSize + " with comment " + comment);
            Lighthouse mLighthouse = new Lighthouse();
            VpnLightHouseServiceClient client = mLighthouse.getVpnLightHouseClient();
            client.newUpdateCustomerLimitCall().call(UpdateCustomerLimitRequest.builder()
                    .withOwnerAccount(accountId)
                    .withPropertyName("Woodchipper.Obelix.instanceType")
                    .withPropertyValue(instanceSize)
                    .withPropertyComment(comment)
                    .build());
            LOGGER.info("Response received. Successfully pinned account " + accountId
                    + " to instance size " + instanceSize + " with comment " + comment);
            return new ClientResponse(
                    "Successfully pinned account " + accountId
                            + " to instance size " + instanceSize
                            + " with comment " + comment,
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
