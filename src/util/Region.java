package com.amazon.aws.vpn.telemetry.horizonte.webapp.util;

import amazon.platform.config.AppConfig;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.commons.io.IOUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Region Class.
 */
@ThreadSafe
public class Region {

    private static final Logger LOGGER = LogManager.getLogger(Region.class);

    private static final String PREFIX = "ip_prefix";

    private static final String PREFIXES = "prefixes";

    private static final String REGION = "region";

    private static final int READ_TIMEOUT_IN_MS = 20 * 1000;

    private static final int CONNECT_TIMEOUT_IN_MS = 5 * 1000;

    private static final String AMAZON_AWS_IP_RANGES_URL = AppConfig.findString("AwsVpnTelemetryHorizonte.rsi_ipranges_url");

    private final LoadingCache<String, JSONArray> subnetInfoCache;

    /**
     * constructor.
     */
    public Region() {
        subnetInfoCache = CacheBuilder.newBuilder()
                .maximumSize(1)
                .expireAfterWrite(1, TimeUnit.DAYS)
                .build(
                        new CacheLoader<String, JSONArray>() {
                            public JSONArray load(String key) {
                                return getIpAddressRanges(key);
                            }
                        });
    }

    /**
     * get region method.
     *
     * @param controlIP input control IP
     * @return region name
     */
    public String getRegion(String controlIP) {
        try {
            LOGGER.info("PERFIXES got");
            if (controlIP.isEmpty()) {
                throw new RuntimeException("input IP is empty");
            }
            JSONArray entries = subnetInfoCache.get(PREFIXES);
            LOGGER.info("JSONArray entries got");
            for (int i = 0; i < entries.length(); i++) {
                JSONObject entry = (JSONObject) entries.get(i);
                if (new SubnetUtils(entry.getString(PREFIX)).getInfo().isInRange(controlIP)) {
                    LOGGER.info("REGION RESULT is " + entry.getString(REGION));
                    return entry.getString(REGION);
                }
            }
            throw new RuntimeException(PREFIX + " is not found");
        } catch (ExecutionException | JSONException e) {
            LOGGER.warn("Unexpected exception while retrieving IP prefix range", e);
            throw new RuntimeException(e);
        }
    }

    private JSONArray getIpAddressRanges(String key) {
        try {
            String jsonString = getIpAddressRangeJson();
            JSONObject data = new JSONObject(jsonString);
            return data.getJSONArray(key);
        } catch (IOException | JSONException e) {
            LOGGER.warn("Unexpected exception while retrieving IP prefix range", e);
            throw new RuntimeException(e);
        }
    }


    private String getIpAddressRangeJson() throws IOException {
        URL url = new URL(AMAZON_AWS_IP_RANGES_URL);
        URLConnection conn = url.openConnection();
        conn.setReadTimeout(READ_TIMEOUT_IN_MS);
        conn.setConnectTimeout(CONNECT_TIMEOUT_IN_MS);
        LOGGER.info("IpAddressRange got");
        return IOUtils.toString(conn.getInputStream(), StandardCharsets.UTF_8);
    }
}
