package com.amazon.aws.vpn.telemetry.horizonte.webapp.util;

import amazon.platform.config.AppConfig;
import com.amazon.appconfigmock.AppConfigMockUtil;

/**
 * This is a AppConfig helper only used for unit test.
 */
public final class AppConfigHelper {

    private AppConfigHelper() {
    }

    /**
     * Set up mock AppConfig.
     */
    public static void setUp() {
        if (AppConfig.isInitialized()) {
            AppConfig.destroy();
        }
        AppConfigMockUtil.mockInitialize("pdx", "test", "App", "Group");
    }
}
