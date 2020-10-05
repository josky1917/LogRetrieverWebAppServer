package com.amazon.aws.vpn.telemetry.horizonte.webapp.controller.core;

import amazon.odin.awsauth.OdinAWSCredentialsProvider;
import amazon.platform.config.AppConfig;
import com.amazon.aws.vpn.telemetry.horizonte.webapp.response.TelemetryResponse;
import com.amazon.aws.vpn.telemetry.horizonte.webapp.util.LdapAuth;
import com.amazon.aws.vpn.telemetry.horizonte.webapp.util.ParseWxOutput;
import com.amazon.aws.vpn.telemetry.horizonte.webapp.util.Region;
import com.amazon.aws.vpn.telemetry.horizonte.webapp.util.Ssh;
import com.amazon.aws.vpn.telemetry.horizonte.webapp.util.User;
import com.amazon.horizonte.spring.annotations.PageType;
import com.amazon.horizonte.spring.stereotype.HorizonteController;
import com.amazon.platform.security.ldap.LdapConnection;
import com.amazonaws.rip.RIPHelper;
import com.jcraft.jsch.Session;
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
import java.util.HashMap;
import java.util.Map;

import static com.amazon.aws.vpn.telemetry.horizonte.webapp.util.Ssh.connect;
import static com.amazon.aws.vpn.telemetry.horizonte.webapp.util.Ssh.sshCmd;

/**
 * This is a Telemetry Controller.
 */
@Controller
@HorizonteController
public class TelemetryController {
    private static final Logger LOGGER = LogManager.getLogger(TelemetryController.class);

    private static final String REGION = RIPHelper.local().region(AppConfig.getRealm().name()).regionName();

    /**
     * Telemetry information from Woodchipper endpoint method.
     * Make SSH connection to the endpoint and retrieve data stream
     *
     * @param request request from react client
     * @return Case Update Response
     */
    @RequestMapping(value = "/TelemetryRequestCall", method = RequestMethod.POST)
    @PageType(pageType = "Telemetry")
    @ResponseBody
    public TelemetryResponse telemetryAPICall(HttpServletRequest request) {
        LOGGER.info("Executing Telemetry api.");
        String userID = (new User()).getUserId(request);
        String controlIp;
        String region;
        String telemetryInfo;
        try {
            LOGGER.info("Receiving request from react client. User ID : " + userID);
            String requestBody = IOUtils.toString(request.getReader());
            JSONObject requestBodyObject = new JSONObject(requestBody);
            controlIp = requestBodyObject.getJSONObject("requestData").getString("controlIp");
            LOGGER.info("Request received.");
        } catch (IOException | JSONException e) {
            LOGGER.error("Request validation failed. " + e.toString(), e);
            return new TelemetryResponse(e, userID);
        }

        try {
            LOGGER.info("Finding region for request control IP " + controlIp);
            Region mRegion = new Region();
            region = mRegion.getRegion(controlIp);
            LOGGER.info("Found region as " + region + " using EC2-IP " + controlIp);
        } catch (Exception e) {
            LOGGER.error("Finding region failed", e);
            return new TelemetryResponse(e, userID);
        }

        LOGGER.info("Checking if region is valid. Currently for "
                + AppConfig.getDomain() + " only " + AppConfig.getRealm().name() + " is valid.");
        if (!region.equals(REGION)) {
            LOGGER.warn("Detected region " + region + " is not " + REGION);
            return new TelemetryResponse(new Exception("Detected region " + region + " is not " + REGION), userID);
        }

        try {
            LOGGER.info("Starting SSH to " + controlIp);
            OdinAWSCredentialsProvider credentials =
                    new OdinAWSCredentialsProvider(
                            AppConfig.findString("AwsVpnTelemetryHorizonte.rsi_material_set_name"));
            Session mSession = connect(
                    controlIp,
                    AppConfig.findString("AwsVpnTelemetryHorizonte.rsi_user"),
                    credentials);
            Ssh.SshCmdResult sshResult = sshCmd(mSession,
                    AppConfig.findString("AwsVpnTelemetryHorizonte.rsi_exec_cmd"));
            telemetryInfo = sshResult.getStdout();
            LOGGER.info("SSH succeeded");
        } catch (IOException e) {
            LOGGER.error("SSH failed", e);
            return new TelemetryResponse(e, userID);
        }

        try {
            LOGGER.info("Parsing Wx Output");

            //check the userId ldap permission
            LdapAuth mLdapAuth = new LdapAuth(new LdapConnection());
            if (mLdapAuth.isUserInAnyLdaps(userID,
                    new String[]{"wx-telemetry-ops", "vpc-looking-glass-ops", "woodchipper"})) {
                LOGGER.info("User " + userID + " has ops access level, getting detail info");
                ParseWxOutput parser = new ParseWxOutput(telemetryInfo);
                LOGGER.info("Get ParseWxOutput.");
                Map<String, String> vpnConfigurationBox = parser.getVpnConfiguration();
                Map<String, String> allLogs = parser.getAllLogs();
                Map<String, String> vpnStatusBox = setVpnStatusBox(parser);
                Map<String, String> countersBox = parser.getCounters();
                Map<String, Map<String, String>> rawLogs = setRawLogs(parser);
                ArrayList<String> ipsecInfoBox = new ArrayList<>();
                if (vpnStatusBox.get("phase1Status").toUpperCase().equals("UP")) {
                    ipsecInfoBox = parser.getOpenSwanInfoPhases();
                } else {
                    ipsecInfoBox.add("No Data");
                }
                Region vgwIpRegion = new Region();
                String regionName = vgwIpRegion.getRegion(vpnConfigurationBox.get("vgwIp"));
                String admiralUrl = parser.constructAdmiralUrl(
                        RIPHelper.local().region(regionName).airportCode().toLowerCase(),
                        vpnConfigurationBox.get("vgwIp"));

                Map<String, Object> wrappedData = new HashMap<>();
                wrappedData.put("Access Level", "Ops");
                wrappedData.put("Headend Version", allLogs.get("HEADEND VERSION"));
                wrappedData.put("VPN Information", vpnConfigurationBox);
                wrappedData.put("Admiral Url", admiralUrl);
                wrappedData.put("VPN Status", vpnStatusBox);
                wrappedData.put("IPSec Info", ipsecInfoBox);
                wrappedData.put("Counters", countersBox);
                wrappedData.put("Logs", sanitizeRawLogs(rawLogs));
                LOGGER.info("WxOutput parser for ops access level done.");
                return new TelemetryResponse(wrappedData, userID);
            } else {
                LOGGER.info("User " + userID + " has restricted access level, getting restricted info");
                String[] rawDataList = telemetryInfo.split("\\*{66}");
                Map<String, Object> restrictedHash = new HashMap<>();
                restrictedHash.put("Access Level", "restricted");
                // Remove first 3 and last 2 elements, and strip remained items from special chars
                for (int i = 0; i < rawDataList.length - 1; i++) {
                    if ((i + 1) % 2 == 0 && !rawDataList[i].contains("LOGS")
                            && !rawDataList[i].contains("XFRM")
                            && !rawDataList[i].contains("DF")
                            && !rawDataList[i].contains("MEMORY")) {
                        restrictedHash.put(
                                rawDataList[i].replaceAll("^[ \t]+|[ \t]+$", ""),
                                rawDataList[i + 1].replaceAll("^[ \t]+|[ \t]+$", "")
                        );
                    }
                }
                LOGGER.info("WxOutput parser for restricted access level done.");
                return new TelemetryResponse(restrictedHash, userID);
            }

        } catch (Exception e) {
            LOGGER.error("Parse Wx Outlogs failed", e);
            return new TelemetryResponse(e, userID);
        }
    }

    private Map<String, String> setVpnStatusBox(ParseWxOutput parser) {
        Map<String, String> vpnStatusBox = new HashMap<>();
        vpnStatusBox.put("uptime", parser.getAllLogs().get("UPTIME"));
        Map<String, String> phasesStatus = parser.getLatestVpnPhasesStatus();
        vpnStatusBox.put("phase1Status", phasesStatus.get("phase1"));
        vpnStatusBox.put("phase2Status", phasesStatus.get("phase2"));
        if (parser.isNatT()) {
            vpnStatusBox.put("natT", "Yes");
        } else {
            vpnStatusBox.put("natT", "No");
        }
        vpnStatusBox.put("bgpState", parser.getBgpState());
        return vpnStatusBox;
    }

    private Map<String, Map<String, String>> setRawLogs(ParseWxOutput parser) {
        Map<String, String> allLogs = parser.getAllLogs();
        Map<String, Map<String, String>> rawLogs = new HashMap<>();

        Map<String, String> routes = new HashMap<>();
        routes.put("Installed Routes", allLogs.get("INSTALLED ROUTES"));
        if (parser.getIsVpnBgp()) {
            routes.put("BGP Routes", allLogs.get("BGP ROUTE INFO"));
        }
        rawLogs.put("Routes", routes);

        Map<String, String> usefulLogs = new HashMap<>();
        usefulLogs.put("OpenSWAN info", allLogs.get("OPENSWAN INFO"));
        usefulLogs.put("OpenSWAN logs", allLogs.get("OPENSWAN LOGS"));
        usefulLogs.put("Strongswan info", allLogs.get("STRONGSWAN INFO"));
        usefulLogs.put("Strongswan logs", allLogs.get("STRONGSWAN LOGS"));
        if (parser.getIsVpnBgp()) {
            usefulLogs.put("BGP neighbor", allLogs.get("BGP NEIGHBOR"));
            usefulLogs.put("BGP logs", allLogs.get("BGP LOGS"));
        }
        if (parser.isRunningVersionGreaterOrEqualThan("1.0-53.23")) {
            usefulLogs.put("XFRM State", allLogs.get("XFRM STATE"));
            usefulLogs.put("XFRM Policy", allLogs.get("XFRM POLICY"));
        } else {
            usefulLogs.put("XFRM Info", allLogs.get("XFRM INFO"));
        }
        usefulLogs.put("XFRM Stats", allLogs.get("XFRM STATS"));
        rawLogs.put("Useful Logs", usefulLogs);

        Map<String, String> systemConfiguration = new HashMap<>();
        systemConfiguration.put("OpenSWAN Configuration", allLogs.get("OPENSWAN CONFIG"));
        systemConfiguration.put("Strongswan Configuration", allLogs.get("STRONGSWAN CONFIG"));
        if (parser.getIsVpnBgp()) {
            systemConfiguration.put("BGP running configuration", allLogs.get("BGP SHOW RUNNING CONFIG"));
        }
        rawLogs.put("System Configuration", systemConfiguration);

        Map<String, String> systemHealth = new HashMap<>();
        systemHealth.put("Uptime", allLogs.get("UPTIME"));
        systemHealth.put("Memory info", allLogs.get("MEMORY INFO"));
        systemHealth.put("Storage usage", allLogs.get("DF"));
        systemHealth.put("System logs", allLogs.get("SYSTEM LOGS"));
        systemHealth.put("Telemetry logs", allLogs.get("TELEMETRY LOGS"));
        rawLogs.put("System Health", systemHealth);

        Map<String, String> systemInterface = new HashMap<>();
        systemInterface.put("VGW outside interface counters", allLogs.get("VGW OUTSIDE INTERFACE COUNTERS"));
        systemInterface.put("Ipsec virtual interface", allLogs.get("IPSEC VIRTUAL INTERFACE"));
        systemInterface.put("Router virtual interface", allLogs.get("ROUTER VIRTUAL INTERFACE"));
        systemInterface.put("South interface", allLogs.get("SOUTH INTERFACE"));
        rawLogs.put("System Interfaces", systemInterface);

        return rawLogs;
    }

    private Map<String, Map<String, String>> sanitizeRawLogs(Map<String, Map<String, String>> rawLogs) {
        Map<String, Map<String, String>> sanitized = new HashMap<>();
        for (Map.Entry<String, Map<String, String>> entry : rawLogs.entrySet()) {
            Map<String, String> tmpSanitized = new HashMap<>();
            for (Map.Entry<String, String> entry2 : entry.getValue().entrySet()) {
                //OPENSWAN or STRONGSWAN might be not exist, so entry2.getValue could be null
                String sanitizeLog = "";
                if (entry2.getValue() != null) {
                    sanitizeLog = entry2.getValue().replaceAll("cgwtunnel.*?\\r\\n",
                            "< Information has been sanitized >");
                }
                tmpSanitized.put(entry2.getKey(), escape(sanitizeLog)); //escape from string to html
            }
            sanitized.put(entry.getKey(), tmpSanitized);
        }
        return sanitized;
    }

    /**
     * Convert String into Html.
     *
     * @param s input string
     * @return html format string
     */
    private static String escape(String s) {
        StringBuilder builder = new StringBuilder();
        boolean previousWasASpace = false;
        for (char c : s.toCharArray()) {
            if (c == ' ') {
                if (previousWasASpace) {
                    builder.append("&nbsp;");
                    previousWasASpace = false;
                    continue;
                }
                previousWasASpace = true;
            } else {
                previousWasASpace = false;
            }
            switch (c) {
                case '<':
                    builder.append("&lt;");
                    break;
                case '>':
                    builder.append("&gt;");
                    break;
                case '&':
                    builder.append("&amp;");
                    break;
                case '"':
                    builder.append("&quot;");
                    break;
                case '\n':
                    builder.append("<br>");
                    break;
                // We need Tab support here, because we print StackTraces as HTML
                case '\t':
                    builder.append("&nbsp; &nbsp; &nbsp;");
                    break;
                default:
                    if (c < 128) {
                        builder.append(c);
                    } else {
                        builder.append("&#").append((int) c).append(";");
                    }
            }
        }
        return builder.toString();
    }
}

