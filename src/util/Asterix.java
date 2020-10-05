package com.amazon.aws.vpn.telemetry.horizonte.webapp.util;

import amazon.odin.awsauth.OdinAWSCredentialsProvider;
import amazon.platform.config.AppConfig;
import com.amazon.coral.client.ClientBuilder;
import woodchipper.asterix.WoodchipperAsterixServiceClient;

/**
 * Asterix initializer.
 */
public class Asterix {

    private final String domain = AppConfig.getDomain();

    private final String realm = AppConfig.getRealm().name();

    /**
     * Get Asterix ServiceClient object.
     *
     * @return obj
     */
    public WoodchipperAsterixServiceClient getAsterixClient() {

        OdinAWSCredentialsProvider credentials =
                new OdinAWSCredentialsProvider(
                        AppConfig.findString("AwsVpnTelemetryHorizonte.asterix_material_set_name"));

        return new CoralIdentity<WoodchipperAsterixServiceClient>(credentials).addToClient(
                WoodchipperAsterixServiceClient.class,
                new ClientBuilder()
                        .remoteOf(WoodchipperAsterixServiceClient.class)
                        .withConfiguration(this.getAsterixClientEndpoint())
                        .newClient()
        );
    }

    /**
     * get endpoint.
     *
     * @return endpoint
     */
    public String getAsterixClientEndpoint() {
        if (this.domain.equals("prod")) {
            return this.domain + "." + this.realm;
        } else {
            return this.domain;
        }
    }
}
