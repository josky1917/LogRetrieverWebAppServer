package com.amazon.aws.vpn.telemetry.horizonte.webapp.util;

import amazon.platform.config.AppConfig;

import javax.servlet.http.HttpServletRequest;

/**
 * User class.
 */
public class User {
    /**
     * get UserId from request header.
     * if midway is disabled, choose default user.
     *
     * @param request httpServletRequest
     * @return UserId
     */
    public String getUserId(HttpServletRequest request) {
        String userID;
        if (AppConfig.findBoolean("AwsVpnTelemetryHorizonte.use_midway")) {
            userID = request.getHeader("x-forwarded-user");
        } else {
            userID = AppConfig.findString("AwsVpnTelemetryHorizonte.default_user");
        }
        return userID;
    }
}
