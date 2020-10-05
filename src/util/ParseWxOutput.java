package com.amazon.aws.vpn.telemetry.horizonte.webapp.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is a woodchipper info parser class.
 */
public class ParseWxOutput {
    private static final Logger LOGGER = LogManager.getLogger(ParseWxOutput.class);

    private final Map<String, String> outPut = new HashMap<>();

    private static final String NO_BGP_TEXT = "No BGP";

    private Boolean isVpnBgp = false;

    /**
     * constructor.
     *
     * @param rawData string input data
     */
    public ParseWxOutput(String rawData) {
        LOGGER.info("Starting initialize rawData");
        LOGGER.info("+++++++++++++++++++++++++++++++++RAW DATA+++++++++++++++++++++++++");
        LOGGER.info(rawData);
        LOGGER.info("+++++++++++++++++++++++++++++++++RAW DATA+++++++++++++++++++++++++");
        // Split sections, there is a delimiter of 66 * chars
        String[] rawDataList = rawData.split("\\*{66}");

        // Remove first 3 and last 2 elements, and strip remained items from special chars
        for (int i = 0; i < rawDataList.length - 5; i++) {
            if ((i + 1) % 2 == 0) {
                LOGGER.info("++++++++++++++++++++++++++++++++++++++++++++");
                LOGGER.info("KEY: " + rawDataList[i + 2].replaceAll("^[ \\t\\r\\n]+|[ \\t\\r\\n]+$", ""));
                this.outPut.put(
                        rawDataList[i + 2].replaceAll("^[ \\t\\r\\n]+|[ \\t\\r\\n]+$", ""),
                        rawDataList[i + 3].replaceAll("^[ \\t\\r\\n]+|[ \\t\\r\\n]+$", "")
                );
            }
        }
        if (getVpnConfiguration().get("vpnType").equals("bgp")) {
            this.isVpnBgp = true;
        }
    }

    /**
     * get all logs.
     *
     * @return logs map
     */
    public Map<String, String> getAllLogs() {
        return this.outPut;
    }

    /**
     * get vpn version.
     *
     * @return string version name
     */
    public String getVpnVersion() {
        return this.outPut.get("HEADEND VERSION");
    }

    private String dataMatcher(String rawData, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(rawData);
        //return first one
        if (matcher.find()) {
            return matcher.group(1).replaceAll("^[ \\t\\r\\n]+|[ \\t\\r\\n]+$", "");
        }
        return "";
    }

    /**
     * get vpn configuration.
     *
     * @return vpn config info map
     */
    public Map<String, String> getVpnConfiguration() {
        String headEndConfiguration = this.outPut.get("HEADEND CONFIG");

        Map<String, String> vpnConfiguration = new HashMap<>();

        vpnConfiguration.put("vpnType", dataMatcher(headEndConfiguration, "INSTANCE TYPE: \"(.*)\""));
        vpnConfiguration.put("cgwIp", dataMatcher(headEndConfiguration, "CGW IP: \"(.*)\""));
        vpnConfiguration.put("vgwIp", dataMatcher(headEndConfiguration, "VGW IP: \"(.*)\""));
        vpnConfiguration.put("env", dataMatcher(headEndConfiguration, "ENVIRONMENT: \"(.*)\""));
        vpnConfiguration.put("region", dataMatcher(headEndConfiguration, "REGION: \"(.*)\""));

        if (isRunningVersionGreaterOrEqualThan("1.0-53.23") || isVpnBgp) {
            vpnConfiguration.put("cgwTunnelIp", dataMatcher(headEndConfiguration, "CGW INSIDE IP: \"(.*)\""));
            vpnConfiguration.put("vgwTunnelIp", dataMatcher(headEndConfiguration, "VGW INSIDE IP: \"(.*)\""));
        }

        if (isVpnBgp) {
            vpnConfiguration.put("cgwAsn", dataMatcher(headEndConfiguration, "CGW BGP AS: \"(.*)\""));
            vpnConfiguration.put("vgwAsn", dataMatcher(headEndConfiguration, "VGW BGP AS: \"(.*)\""));
        }

        return vpnConfiguration;
    }

    /**
     * check if vpn is bgp.
     *
     * @return boolean
     */
    public boolean getIsVpnBgp() {
        return this.isVpnBgp;
    }

    /**
     * get bgp state.
     *
     * @return string state
     */
    public String getBgpState() {
        String bgpState;
        if (this.isVpnBgp) {
            String bgpNeighbor = this.outPut.get("BGP NEIGHBOR");
            bgpState = dataMatcher(bgpNeighbor, "BGP state = (.*)");
        } else {
            bgpState = NO_BGP_TEXT;
        }
        return bgpState;
    }

    /**
     * get counters info.
     *
     * @return counters map
     */
    public Map<String, String> getCounters() {
        String woodchipperRoutingStats = this.outPut.get("WOODCHIPPER ROUTING STATS");

        Map<String, String> counters = new HashMap<>();

        counters.put("tunnelTxPackets", dataMatcher(woodchipperRoutingStats, "TunnelTxPackets\t(\\d*)"));
        counters.put("tunnelTxBytes", dataMatcher(woodchipperRoutingStats, "TunnelTxBytes\t(\\d*)"));
        counters.put("tunnelRxPackets", dataMatcher(woodchipperRoutingStats, "TunnelRxPackets\\t(\\d*)"));
        counters.put("tunnelRxBytes", dataMatcher(woodchipperRoutingStats, "TunnelRxBytes\t(\\d*)"));
        counters.put("otherTxPackets", dataMatcher(woodchipperRoutingStats, "VpcEncapTxPackets\\t(\\d*)"));
        counters.put("otherTxBytes", dataMatcher(woodchipperRoutingStats, "VpcEncapTxBytes\t(\\d*)"));
        counters.put("otherRxPackets", dataMatcher(woodchipperRoutingStats, "VpcEncapRxPackets\t(\\d*)"));
        counters.put("otherRxBytes", dataMatcher(woodchipperRoutingStats, "VpcEncapRxBytes\t(\\d*)"));
        counters.put("cxVpcTxPackets", dataMatcher(woodchipperRoutingStats, "VpcTxPackets\t(\\d*)"));
        counters.put("cxVpcTxBytes", dataMatcher(woodchipperRoutingStats, "VpcTxBytes\t(\\d*)"));
        counters.put("cxVpcRxPackets", dataMatcher(woodchipperRoutingStats, "VpcRxPackets\t(\\d*)"));
        counters.put("cxVpcRxBytes", dataMatcher(woodchipperRoutingStats, "VpcRxBytes\t(\\d*)"));

        String installedRoutes = this.outPut.get("INSTALLED ROUTES");

        counters.put("installedRoutes", dataMatcher(installedRoutes, "Number of routes = (.*)"));

        if (this.isVpnBgp) {
            String bgpRoutes = this.outPut.get("BGP ROUTE INFO");
            counters.put("bgpTotalPrefixes", dataMatcher(bgpRoutes, "Total number of prefixes (\\d*)"));
        } else {
            counters.put("bgpTotalPrefixes", NO_BGP_TEXT);
        }
        return counters;
    }

    /**
     * get lastest vpn phases status (IkeP1State & IkeP2State).
     *
     * @return phases status map
     */
    public Map<String, String> getLatestVpnPhasesStatus() {
        Map<String, String> phasesStatus = new HashMap<>();
        String phase1 = "";
        String phase2 = "";

        String[] splitList = this.outPut.get("TELEMETRY LOGS").split("Posted self check");
        for (int i = splitList.length - 1; i > 0; i--) {
            if (!splitList[i].contains("u'IkeP1State'")) {
                phase1 = "N/A";
                phase2 = "N/A";
            } else {
                phase1 = dataMatcher(splitList[i], "u'IkeP1State': u'([a-zA-Z]+)'");
                phase2 = dataMatcher(splitList[i], "u'IkeP2State': u'([a-zA-Z]+)'");
            }
        }
        phasesStatus.put("phase1", phase1);
        phasesStatus.put("phase2", phase2);
        return phasesStatus;
    }

    /**
     * get openswan or strongswan info phase.
     *
     * @return phase array list
     */
    public ArrayList<String> getOpenSwanInfoPhases() {
        ArrayList<String> openSwanInfoPhase = new ArrayList<>();
        String[] openSwanInfo;
        if (this.outPut.containsKey("OPENSWAN INFO")) {
            openSwanInfo = this.outPut.get("OPENSWAN INFO").split("\\r\\n");
        } else {
            openSwanInfo = this.outPut.get("STRONGSWAN INFO").split("\\r\\n");
        }

        String cgwIp = getVpnConfiguration().get("cgwIp");
        for (String s : openSwanInfo) {
            if (s.contains(cgwIp)) {
                openSwanInfoPhase.add(s);
            }
        }
        return openSwanInfoPhase;
    }

    /**
     * get version (major & minor).
     *
     * @return version map
     */
    public Map<String, String> getNumericVpnVersion() {
        String stringVersion = getVpnVersion();

        Map<String, String> wxVersion = new HashMap<>();

        String[] splitVersion = stringVersion.split("-");
        wxVersion.put("majorVersion", splitVersion[1]);
        wxVersion.put("minorVersion", splitVersion[2]);

        return wxVersion;
    }

    /**
     * check if given version is >= running version.
     *
     * @param requiredVersion version name
     * @return boolean
     */
    public Boolean isRunningVersionGreaterOrEqualThan(String requiredVersion) {
        Map<String, String> runningVersion = getNumericVpnVersion();

        String[] splitRequiredVersionList = requiredVersion.split("-");
        Map<String, String> splitRequiredVersion = new HashMap<>();
        splitRequiredVersion.put("majorVersion", splitRequiredVersionList[0]);
        splitRequiredVersion.put("minorVersion", splitRequiredVersionList[1]);

        return Double.parseDouble(runningVersion.get("majorVersion"))
                > Double.parseDouble(splitRequiredVersion.get("majorVersion"))
                || (Double.parseDouble(runningVersion.get("majorVersion"))
                == Double.parseDouble(splitRequiredVersion.get("majorVersion"))
                && Double.parseDouble(runningVersion.get("minorVersion"))
                >= Double.parseDouble(splitRequiredVersion.get("minorVersion")));
    }

    /**
     * check if use NAT-T.
     *
     * @return boolean
     */
    public boolean isNatT() {
        boolean isNat = false;
        ArrayList<String> tmpOpenSwanInfo = getOpenSwanInfoPhases();
        String cgwIp = getVpnConfiguration().get("cgwIp");

        for (String s : tmpOpenSwanInfo) {
            if (s.contains(cgwIp + ":4500")) {
                isNat = true;
                break;
            }
        }
        return isNat;
    }

    /**
     * construct Admiral Url.
     *
     * @param airportCode region name
     * @param ip          ip address
     * @return url
     */
    public String constructAdmiralUrl(String airportCode, String ip) {
        return "https://admiral-" + airportCode + ".ec2.amazon.com/search?q=" + ip;
    }

}
