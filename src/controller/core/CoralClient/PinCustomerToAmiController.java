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
public class PinCustomerToAmiController {
    private static final Logger LOGGER = LogManager.getLogger(PinCustomerToAmiController.class);

    /**
     * Pin Customer To Ami API call controller method.
     *
     * @param request request from react client
     * @return Case Update Response
     */
    @RequestMapping(value = "/PinCustomerToAmiRequestCall", method = RequestMethod.POST)
    @PageType(pageType = "PinCustomerToAmi")
    @ResponseBody
    public ClientResponse pinCustomerToAmiAPICall(HttpServletRequest request) {
        LOGGER.info("Executing Pin Customer To Ami api.");
        String userID = (new User()).getUserId(request);
        String accountId;
        String amiId;
        String comment;
        try {
            LOGGER.info("Receiving request from react client. User ID : " + userID);
            String requestBody = IOUtils.toString(request.getReader());
            JSONObject requestBodyObject = new JSONObject(requestBody);
            accountId = requestBodyObject.getJSONObject("requestData").getString("accountId");
            amiId = requestBodyObject.getJSONObject("requestData").getString("amiId");
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
            LOGGER.info(userID + " is pinning account " + accountId + " to ami "
                    + amiId + " with comment " + comment);
            Lighthouse mLighthouse = new Lighthouse();
            VpnLightHouseServiceClient client = mLighthouse.getVpnLightHouseClient();
            client.newUpdateCustomerLimitCall().call(UpdateCustomerLimitRequest.builder()
                    .withOwnerAccount(accountId)
                    .withPropertyName("Woodchipper.Obelix.vpnAmiId")
                    .withPropertyValue(amiId)
                    .withPropertyComment(comment)
                    .build());
            LOGGER.info("Response received. Successfully pinned account " + accountId
                    + " to ami " + amiId + " with comment " + comment);
            return new ClientResponse(
                    "Successfully pinned account " + accountId
                            + " to ami " + amiId
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

