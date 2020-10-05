package com.amazon.aws.vpn.telemetry.horizonte.webapp.response;

import lombok.Data;

import java.util.HashMap;

/**
 * LdapResponse.
 * isLdapAccess: a basic check for user if this user is in any of LDAP groups permitted by telemtry.
 * ldapGroup: a Map<String each LDAP group name, Boolean if user is in this LDAP group>
 * exception: error massage.
 * requesterID: Current user ID.
 */
@Data
public class LdapResponse {
    private static final String DEFAULT_ERROR_MSG = "Unexpected exception occurred. Please contact woodchipper team for help.";

    private final Boolean isLdapAccess;
    private final HashMap<String, Boolean> ldapGroup;
    private final String exception;
    private final String requesterID;

    /**
     * Exception occurred.
     *
     * @param e Exception
     * @param requesterID requesterId
     */
    public LdapResponse(Exception e, String requesterID) {
        this(null, null, e, requesterID);
    }

    /**
     * output if ldap groups are accessible.
     *
     * @param isLdapAccess isLdapAccess
     * @param ldapGroup ldapGroup
     * @param requesterID requesterID
     */
    public LdapResponse(Boolean isLdapAccess, HashMap<String, Boolean> ldapGroup, String requesterID) {
        this(isLdapAccess, ldapGroup, null, requesterID);
    }

    /**
     * Main constructor.
     *
     * @param isLdapAccess isLdapAccess
     * @param ldapGroup ldapGroup
     * @param e exception
     * @param requesterID requesterID
     */
    public LdapResponse(Boolean isLdapAccess, HashMap<String, Boolean> ldapGroup, Exception e, String requesterID) {
        this.isLdapAccess = isLdapAccess;
        this.ldapGroup = ldapGroup;
        this.exception = e != null
                ? DEFAULT_ERROR_MSG
                : "";
        this.requesterID = requesterID;
    }
}
