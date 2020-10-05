package com.amazon.aws.vpn.telemetry.horizonte.webapp.controller.core;

import com.amazon.aws.vpn.telemetry.horizonte.webapp.response.LdapResponse;
import com.amazon.aws.vpn.telemetry.horizonte.webapp.util.LdapAuth;
import com.amazon.aws.vpn.telemetry.horizonte.webapp.util.User;
import com.amazon.horizonte.spring.annotations.PageType;
import com.amazon.platform.security.ldap.LdapConnection;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;

/**
 * This is Ldap Auth Controller.
 */
@Controller
public class LdapRequestController {

    /**
     * Ldap Auth API call controller method.
     *
     * @param request request from react client
     * @return Case Update Response
     */
    @RequestMapping(value = "/LdapRequestCall", method = RequestMethod.POST)
    @PageType(pageType = "LdapRequest")
    @ResponseBody
    public LdapResponse ldapAPICall(HttpServletRequest request) {
        String userID = (new User()).getUserId(request);
        JSONArray ldapGroupNames;
        try {
            String requestBody = IOUtils.toString(request.getReader());
            JSONObject requestBodyObject = new JSONObject(requestBody);
            ldapGroupNames = requestBodyObject.getJSONArray("ldapGroupName");
        } catch (IOException | JSONException e) {
            return new LdapResponse(new Exception("Invalid request for Ldap access"), userID);
        }
        LdapAuth mLdapAuth = new LdapAuth(new LdapConnection());
        Boolean isLdapAccess = false;
        HashMap<String, Boolean> ldapGroups = new HashMap<String, Boolean>();
        for (int i = 0; i < ldapGroupNames.length(); i++) {
            ldapGroups.put(ldapGroupNames.optString(i), false);
            if (mLdapAuth.hasAccessToPage(request, ldapGroupNames.optString(i))) {
                isLdapAccess = true;
                ldapGroups.replace(ldapGroupNames.optString(i), true);
            }
        }
        return new LdapResponse(isLdapAccess, ldapGroups, userID);
    }
}
