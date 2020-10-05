package com.amazon.aws.vpn.telemetry.horizonte.webapp.controller.core.CoralClient;

import com.amazon.aws.vpn.telemetry.horizonte.webapp.response.ClientResponse;
import com.amazon.aws.vpn.telemetry.horizonte.webapp.util.Lighthouse;
import com.amazon.aws.vpn.telemetry.horizonte.webapp.util.User;
import com.amazon.horizonte.spring.annotations.PageType;
import com.amazon.horizonte.spring.stereotype.HorizonteController;
import com.amazon.vpnlighthouse.RunCommandTcpdumpRequest;
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
 * This is a Tcp Dump Controller.
 */
@Controller
@HorizonteController
public class TcpDumpController {
    private static final Logger LOGGER = LogManager.getLogger(TcpDumpController.class);

    /**
     * Tcp Dump API call controller method.
     *
     * @param request request from react client
     * @return Case Update Response
     */
    @RequestMapping(value = "/TcpDumpRequestCall", method = RequestMethod.POST)
    @PageType(pageType = "TcpDump")
    @ResponseBody
    public ClientResponse tcpDumpAPICall(HttpServletRequest request) {
        LOGGER.info("Executing tcp dump api.");
        String userID = (new User()).getUserId(request);
        String vpnId;
        String managementIp;
        String protocol;
        int packetCount;
        String listenOnInterface;
        String port;
        try {
            LOGGER.info("Receiving request from react client. User Id: " + userID);
            String requestBody = IOUtils.toString(request.getReader());
            JSONObject requestBodyObject = new JSONObject(requestBody);
            vpnId = requestBodyObject.getJSONObject("requestData").getString("vpnId");
            managementIp = requestBodyObject.getJSONObject("requestData").getString("managementIp");
            protocol = requestBodyObject.getJSONObject("requestData").getString("protocol");
            packetCount = requestBodyObject.getJSONObject("requestData").getInt("packetCount");
            listenOnInterface = requestBodyObject.getJSONObject("requestData").getString("listenOnInterface");
            port = requestBodyObject.getJSONObject("requestData").getString("port");
            LOGGER.info("Request received.");
        } catch (IOException | JSONException e) {
            LOGGER.error("Request validation failed. " + e.toString(), e);
            return new ClientResponse(
                    "Request is not valid",
                    new Exception("Invalid request for TcpDump operation"),
                    userID);
        }

        try {
            LOGGER.info(userID + " is requesting tcpdump with vpnId " + vpnId + ", management ip " + managementIp
                    + ", protocol " + protocol + ", packet count " + packetCount
                    + ", listen on interface (if has): " + listenOnInterface
                    + ", prot (if has): " + port);
            Lighthouse mLighthouse = new Lighthouse();
            VpnLightHouseServiceClient client = mLighthouse.getVpnLightHouseClient();
            SsmRunCommandResponse response = client.newRunCommandTcpdumpCall().call(RunCommandTcpdumpRequest.builder()
                    .withRequesterLogin(userID)
                    .withVpnId(vpnId)
                    .withManagementIp(managementIp)
                    .withProtocol(protocol)
                    .withPacketCount(packetCount)
                    .withListenOnInterface(listenOnInterface.isEmpty() ? null : listenOnInterface)
                    .withPort(port.isEmpty() ? null : Integer.parseInt(port))
                    .build());
            LOGGER.info("Response received with S3 Bucket Name of tcpdump request for " + userID
                    + " is " + response.getS3BucketName());
            return new ClientResponse(
                    "Successfully sent a tcpdump request to Vpn ID " + vpnId + " and " + managementIp
                            + " with protocol " + protocol
                            + ". S3 Bucket Name of tcpdump request for " + userID
                            + " is " + response.getS3BucketName(),
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
