package com.amazon.aws.vpn.telemetry.horizonte.webapp.util;

import amazon.platform.config.AppConfig;
import com.amazon.midway.filter.MidwayProxyDelegationFilter;
import com.amazon.platform.security.ldap.LdapConnection;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;

/**
 * Ldap Authentication.
 */
public class LdapAuth {

    /**
     * SSL URL for LDAP server.
     */
    private static final String LDAP_SSL_URL = "ldap://ldap.amazon.com:389";

    /**
     * Default User for developing in localhost.
     */
    private static final String DEFAULT_USER = AppConfig.findString("AwsVpnTelemetryHorizonte.default_user");

    /**
     * Ldap connection.
     */
    private final LdapConnection mConnection;

    /**
     * The constructor.
     *
     * @param connection Ldap connection
     */
    public LdapAuth(final LdapConnection connection) {
        this.mConnection = connection;
    }

    /**
     * Verifies if a user Id is a part of the LDAP group.
     *
     * @param req           the HttpServletRequest
     * @param ldapGroupName the groups needed to be checked
     * @return is the user in ldap
     */
    public boolean hasAccessToPage(final HttpServletRequest req, final String ldapGroupName) {
        final String userId = getUserId(req);
        final String mLdapGroup = AppConfig.findString("AwsVpnTelemetryHorizonte.ldap_group." + ldapGroupName);
        try {
            mConnection.open(LDAP_SSL_URL);
            return mConnection.isUserInLdapGroup(userId, mLdapGroup);
        } catch (NamingException e) {
            return false;
        } finally {
            try {
                mConnection.close();
            } catch (NamingException e) {
                return false;
            }
        }
    }

    /**
     * Returns the UserId of the user.
     *
     * @param request - request sent to access resources within the application
     * @return UserId of the logged in user
     */
    public String getUserId(final HttpServletRequest request) {
        Validate.notNull(request);
        String user;
        if (AppConfig.findBoolean("AwsVpnTelemetryHorizonte.use_midway")) {
            user = request.getHeader(MidwayProxyDelegationFilter.MIDWAY_PROXY_AUTH_IDENTITY_HEADER);
            if (StringUtils.isBlank(user)) {
                user = DEFAULT_USER;
            }
        } else {
            user = DEFAULT_USER;
        }
        return user;
    }

    /**
     * check if user have any permission for a list of ldap group.
     * @param userId user Id
     * @param groupNames Ldap group list
     * @return boolean
     */
    public boolean isUserInAnyLdaps(String userId, String[] groupNames) {
        try {
            mConnection.open(LDAP_SSL_URL);
            for (String groupName : groupNames) {
                if (mConnection.isUserInLdapGroup(userId, groupName)) {
                    return true;
                }
            }
            return false;
        } catch (NamingException e) {
            return false;
        } finally {
            try {
                mConnection.close();
            } catch (NamingException e) {
                return false;
            }
        }
    }
}
