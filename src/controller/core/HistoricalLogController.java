package com.amazon.aws.vpn.telemetry.horizonte.webapp.controller.core;

import amazon.platform.config.AppConfig;
import com.amazon.aws.vpn.telemetry.horizonte.webapp.response.HistoricalLogResponse;
import com.amazon.aws.vpn.telemetry.horizonte.webapp.util.Region;
import com.amazon.aws.vpn.telemetry.horizonte.webapp.util.TimberLogRetriever;
import com.amazon.aws.vpn.telemetry.horizonte.webapp.util.User;
import com.amazon.horizonte.spring.annotations.PageType;
import com.amazon.horizonte.spring.stereotype.HorizonteController;
import com.amazonaws.rip.RIPHelper;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * This is a Historical Log Controller.
 * Receive request from UI
 * Response object with Log info to UI
 */
@Controller
@HorizonteController
public class HistoricalLogController {
    private static final Logger LOGGER = LogManager.getLogger(HistoricalLogController.class);

    private static final String REGION = RIPHelper.local().region(AppConfig.getRealm().name()).regionName();

    /**
     * Historical Log API call controller method.
     *
     * @param request request from react client
     * @return Case Update Response
     */
    @RequestMapping(value = "/HistoricalLogRequestCall", method = RequestMethod.POST)
    @PageType(pageType = "HistoricalLog")
    @ResponseBody
    public HistoricalLogResponse historicalLogAPICall(HttpServletRequest request) {
        LOGGER.info("Executing Historical Log api.");
        String userID = (new User()).getUserId(request);
        String startDate;
        String cgwIp;
        String vgwIp;
        String region;

        try {
            LOGGER.info("Receiving request from react client. User ID : " + userID);
            String requestBody = IOUtils.toString(request.getReader());
            JSONObject requestBodyObject = new JSONObject(requestBody);
            startDate = requestBodyObject.getJSONObject("requestData").getString("startTime");
            cgwIp = requestBodyObject.getJSONObject("requestData").getString("cgwIp");
            vgwIp = requestBodyObject.getJSONObject("requestData").getString("vgwIp");
            checkNotNull(startDate);
            checkNotNull(cgwIp);
            checkNotNull(vgwIp);
            LOGGER.info("Request received.");
        } catch (IOException | JSONException e) {
            LOGGER.error("Request validation failed. " + e.toString(), e);
            return new HistoricalLogResponse(
                    null,
                    new Exception("Invalid request for Historical Log operation"),
                    userID);
        }
        try {
            LOGGER.info("Finding region for request vgw IP " + vgwIp);
            Region mRegion = new Region();
            region = mRegion.getRegion(vgwIp);
            LOGGER.info("Found region as " + region + " using VGW IP " + vgwIp);
        } catch (Exception e) {
            LOGGER.error("Finding region failed", e);
            return new HistoricalLogResponse(e, userID);
        }

        LOGGER.info("Checking if region is valid. Currently for beta PDX host only pdx is available."
                + "For gamma IAD hsot only IAD is available.");
        if (!region.equals(REGION)) {
            LOGGER.warn("Region " + region + " is not " + AppConfig.getRealm().name());
            return new HistoricalLogResponse(new Exception("Region " + region + " is not "
                    + AppConfig.getRealm().name()), userID);
        }
        try {
            LOGGER.info("Retrieving Logs... startDate: " + startDate
                    + " cgwIp: " + cgwIp + " vgwIp: " + vgwIp);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date startTime = dateFormat.parse(startDate);

            Map<String, String> searchableKey = new HashMap<>();
            searchableKey.put("cgw-ip", cgwIp.trim());
            searchableKey.put("vgw-ip", vgwIp.trim());
            TimberLogRetriever timberLogRetriever = new TimberLogRetriever();
            LOGGER.info("TimberLogRetriever is initialized");
            ArrayList<Map<String, String>> logsList = timberLogRetriever.getLogs(startTime, searchableKey);
            LOGGER.info("Log Map is created");
            return new HistoricalLogResponse(logsList, userID);

        } catch (Exception e) {
            LOGGER.error("Request declined. " + e.toString(), e);
            return new HistoricalLogResponse(e, userID);
        }
    }
}

