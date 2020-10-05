package com.amazon.aws.vpn.telemetry.horizonte.webapp.controller.core.CoralClient;

import com.amazon.aws.vpn.telemetry.horizonte.webapp.response.ClientResponse;
import com.amazon.aws.vpn.telemetry.horizonte.webapp.util.Lighthouse;
import com.amazon.aws.vpn.telemetry.horizonte.webapp.util.User;
import com.amazon.horizonte.spring.annotations.PageType;
import com.amazon.horizonte.spring.stereotype.HorizonteController;
import com.amazon.vpnlighthouse.CustomerLimit;
import com.amazon.vpnlighthouse.FindCustomerLimitRequest;
import com.amazon.vpnlighthouse.FindCustomerLimitsResult;
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
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang.StringUtils.join;

/**
 * This is a Get Customer Limit Controller.
 */
@Controller
@HorizonteController
public class GetCustomerLimitController {
    private static final Logger LOGGER = LogManager.getLogger(GetCustomerLimitController.class);

    /**
     * Get Customer Limit API call controller method.
     *
     * @param request request from react client
     * @return Case Update Response
     */
    @RequestMapping(value = "/GetCustomerLimitRequestCall", method = RequestMethod.POST)
    @PageType(pageType = "GetCustomerLimit")
    @ResponseBody
    public ClientResponse getCustomerLimitAPICall(HttpServletRequest request) {
        LOGGER.info("Executing get customer limit api.");
        String userID = (new User()).getUserId(request);
        String accountId;
        String propertyName;
        try {
            LOGGER.info("Receiving request from react client. User Id: " + userID);
            String requestBody = IOUtils.toString(request.getReader());
            JSONObject requestBodyObject = new JSONObject(requestBody);
            accountId = requestBodyObject.getJSONObject("requestData").getString("accountId");
            propertyName = requestBodyObject.getJSONObject("requestData").getString("propertyName");
            LOGGER.info("Request received.");
        } catch (IOException | JSONException e) {
            LOGGER.error("Request validation failed. " + e.toString(), e);
            return new ClientResponse(
                    "Request is not valid",
                    new Exception("Invalid request for GetCustomerLimit operation"),
                    userID);
        }

        try {
            LOGGER.info(userID + " is removing customer limit for " + accountId
                    + " with property name " + propertyName);
            Lighthouse mLighthouse = new Lighthouse();
            VpnLightHouseServiceClient client = mLighthouse.getVpnLightHouseClient();
            FindCustomerLimitsResult response = client.newFindCustomerLimitsCall().call(FindCustomerLimitRequest.builder()
                    .withOwnerAccount(accountId)
                    .withPropertyName(propertyName)
                    .build());
            List<CustomerLimit> responseCustomerLimitList = response.getCustomerLimits();
            List<String> responsePropertyNameList = new ArrayList<>();
            for (CustomerLimit each : responseCustomerLimitList) {
                responsePropertyNameList.add(each.getPropertyValue());
            }
            LOGGER.info("Response received with property names "
                    + join(responsePropertyNameList.toArray(), ","));
            return new ClientResponse(
                    "The customer limit for " + accountId
                            + " with property name " + propertyName
                            + " is " + join(responsePropertyNameList.toArray(), ","),
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
