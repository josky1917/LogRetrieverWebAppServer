package com.amazon.aws.vpn.telemetry.horizonte.webapp.controller.core.CoralClient;


import com.amazon.aws.vpn.telemetry.horizonte.webapp.response.ClientResponse;
import com.amazon.aws.vpn.telemetry.horizonte.webapp.util.Asterix;
import com.amazon.aws.vpn.telemetry.horizonte.webapp.util.User;
import com.amazon.horizonte.spring.annotations.PageType;
import com.amazon.horizonte.spring.stereotype.HorizonteController;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import woodchipper.asterix.SetWorkflowStateRequest;
import woodchipper.asterix.SetWorkflowStateResponse;
import woodchipper.asterix.WoodchipperAsterixServiceClient;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * This is a Set Workflow State Controller.
 */
@Controller
@HorizonteController
public class SetWorkflowStateController {
    private static final Logger LOGGER = LogManager.getLogger(SetWorkflowStateController.class);

    /**
     * Set Workflow State API call controller method.
     *
     * @param request request from react client
     * @return Case Update Response
     */
    @RequestMapping(value = "/SetWorkflowStateRequestCall", method = RequestMethod.POST)
    @PageType(pageType = "SetWorkflowState")
    @ResponseBody
    public ClientResponse setWorkflowStateAPICall(HttpServletRequest request) {
        LOGGER.info("Executing replace instance api.");
        String userID = (new User()).getUserId(request);
        String ownerAccount;
        String rowType;
        long rowId;
        String oldState;
        String newState;
        String forcedStateChange;
        try {
            LOGGER.info("Receiving request from react client. User ID : " + userID);
            String requestBody = IOUtils.toString(request.getReader());
            JSONObject requestBodyObject = new JSONObject(requestBody);
            ownerAccount = requestBodyObject.getJSONObject("requestData").getString("ownerAccount");
            rowType = requestBodyObject.getJSONObject("requestData").getString("rowType");
            rowId = requestBodyObject.getJSONObject("requestData").getLong("rowId");
            oldState = requestBodyObject.getJSONObject("requestData").getString("oldState");
            newState = requestBodyObject.getJSONObject("requestData").getString("newState");
            forcedStateChange = requestBodyObject.getJSONObject("requestData").getString("forcedStateChange");
            LOGGER.info("Request received.");
        } catch (IOException | JSONException e) {
            LOGGER.error("Request validation failed. " + e.toString(), e);
            return new ClientResponse(
                    "Request is not valid", new Exception("Invalid request for ReplaceInstance operation"), userID);
        }

        try {
            LOGGER.info(userID + " is setting workflow state for  owner account "
                    + ownerAccount + " with row type " + rowType + " with row id " + rowId
                    + " with old state " + oldState + " with new state " + newState);
            Asterix mAsterix = new Asterix();
            WoodchipperAsterixServiceClient client = mAsterix.getAsterixClient();
            SetWorkflowStateRequest asterixRequest = new SetWorkflowStateRequest();
            asterixRequest.setOwnerAccount(ownerAccount);
            asterixRequest.setRowType(rowType);
            asterixRequest.setRowId(rowId);
            asterixRequest.setOldState(oldState);
            asterixRequest.setNewState(newState);
            asterixRequest.setForcedStateChange(forcedStateChange.toLowerCase().equals("true"));
            SetWorkflowStateResponse response = client.newSetWorkflowStateCall().call(asterixRequest);
            LOGGER.info("Response received. Current state of set workflow state response for "
                    + userID + " is " + response.getCurrentState());
            LOGGER.info("Number of workflows updated of set workflow state response for "
                    + userID + " is " + response.getWorkflowsUpdated());
            return new ClientResponse(
                    response.getWorkflowsUpdated() + "workflows have been updated with a current state of "
                            + response.getCurrentState(),
                    userID);
        } catch (Exception e) {
            LOGGER.error("Request declined. " + e.toString(), e);
            return new ClientResponse(
                    "Asterix Request Decline\n" + e.toString(),
                    e,
                    userID);
        }
    }
}
