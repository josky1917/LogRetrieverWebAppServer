package com.amazon.aws.vpn.telemetry.horizonte.webapp.util;

import amazon.odin.awsauth.OdinAWSCredentialsProvider;
import amazon.platform.config.AppConfig;
import com.amazon.coral.client.ClientBuilder;
import com.amazon.vpnlighthouse.VpnLightHouseServiceClient;

/**
 * Lighthouse initializer.
 */
public class Lighthouse {

    private final String domain = AppConfig.getDomain();

    private final String realm = AppConfig.getRealm().name();

    /**
     * Get LighthoueServiceClient object.
     *
     * @return obj
     */
    public VpnLightHouseServiceClient getVpnLightHouseClient() {

        OdinAWSCredentialsProvider credentials =
                new OdinAWSCredentialsProvider(
                        AppConfig.findString("AwsVpnTelemetryHorizonte.lighthouse_material_set_name"));

        return new CoralIdentity<VpnLightHouseServiceClient>(credentials).addToClient(
                VpnLightHouseServiceClient.class,
                new ClientBuilder()
                        .remoteOf(VpnLightHouseServiceClient.class)
                        .withConfiguration(this.getVpnLightHouseServiceClientEndpoint())
                        .newClient()
        );
    }

    /**
     * get endpoint.
     *
     * @return endpoint
     */
    public String getVpnLightHouseServiceClientEndpoint() {
        switch (this.domain) {
            case "beta":
                return "Base.Beta";
            case "gamma":
                return "Base.Gamma";
            default:
                return "Base.Prod." + this.realm;
        }
    }
}
