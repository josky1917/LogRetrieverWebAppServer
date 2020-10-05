package com.amazon.aws.vpn.telemetry.horizonte.webapp.config;

import com.amazon.cloudauth.client.CloudAuthCredentials;
import com.amazon.cloudauth.client.resourceserver.Bootstrapper;
import com.amazon.core.platform.api.data.descriptor.DataConfigurationState;
import com.amazon.core.platform.cloudauth.api.CloudAuthClientCredentials;
import com.amazon.core.platform.cloudauth.api.CloudAuthResourceMapper;
import com.amazon.core.platform.cloudauth.api.CloudAuthResourceServerBootstrapper;
import com.amazon.core.platform.cloudauth.api.CloudAuthResourceServerCredentials;
import com.amazon.core.platform.cloudauth.configuration.CloudAuthDataSourceConfiguration;
import com.amazon.core.platform.csrf.odin.runtime.OdinCsrfImplementationsFactory;
import com.amazon.core.platform.identity.services.runtime.StaticCookieNameDataConfiguration;
import com.amazon.core.platform.runtime.configuration.PlatformDataSourceDefaultConfiguration;
import com.amazon.core.platform.runtime.configuration.PlatformStaticMarketplaceConfiguration;
import com.amazon.core.platform.runtime.configuration.PlatformStaticSessionConfiguration;
import com.amazon.core.platform.security.runtime.csrf.configuration.CsrfDataConfiguration;
import com.amazon.core.platform.spi.data.descriptor.DataConfiguration;
import com.amazon.core.platform.spi.data.descriptor.StaticGlobalDataSourceDescriptor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * This class is where you can override existing DataSources and add custom DataSources. If your custom DataSources are
 * configured via an implementation of DataConfiguration  you can chain that configuration into the default
 * PlatformDataSourceDefaultConfiguration. For more information, please go to
 * https://w.amazon.com/index.php/Horizonte_5.0/Develop_Horizonte_web_app_201#Override_the_Default_Platform_Configuration
 */
public class CustomDataConfigurationOverrides implements DataConfiguration {

    private static final String CSRF_SERVICE_ID = "0x83";

    private static final String MARKETPLACE_ID = "ATVPDKIKX0DER";

    private static final Optional<String> DEFAULT_NAME = Optional.of("CustomMarketplace");
    private static final Optional<String> DEFAULT_ID = Optional.of(MARKETPLACE_ID);
    private static final Optional<String> DEFAULT_OWNER = Optional.of("A1OHOT6VONX3KA");
    private static final Optional<List<String>> DEFAULT_MERCHANTS = Optional.of(Collections.singletonList("AV6NJB5ZEWT7A"));
    private static final Optional<List<String>> DEFAULT_LANGUAGES = Optional.of(Collections.singletonList("EN-US"));
    private static final Optional<String> DEFAULT_LANGUAGE = Optional.of("EN-US");
    private static final String STATIC_SESSION_ID = "123-456-7890";
    private static final String STATIC_CUSTOMER_ID = "abcd";
    private static final String STATIC_SESSION_LANGUAGE = "en-US";

    //Additional DataConfiguration classes can be chained using .before(new CustomDataConfiguration())
    private final DataConfiguration innerConfiguration = new PlatformDataSourceDefaultConfiguration()
            .before(new PlatformStaticSessionConfiguration(STATIC_SESSION_ID, STATIC_CUSTOMER_ID,
                    STATIC_SESSION_LANGUAGE))
            .before(new PlatformStaticMarketplaceConfiguration(DEFAULT_NAME, DEFAULT_ID, DEFAULT_OWNER,
                    DEFAULT_MERCHANTS, DEFAULT_LANGUAGES, DEFAULT_LANGUAGE))
            .before(new StaticCookieNameDataConfiguration(MARKETPLACE_ID))
            .before(CsrfDataConfiguration.newGlobalStaticMapBuilder()
                    .addOrReplace(CSRF_SERVICE_ID, OdinCsrfImplementationsFactory.defaultOx83TokenServiceImpl(
                            System.getProperty("amazon.csrf.material.set.name.0x83"),
                            Integer.getInteger("amazon.csrf.material.set.serial.0x83")).get())
                    .withDefaultId(CSRF_SERVICE_ID)
                    .build())
            .before(CloudAuthDataSourceConfiguration.builder()
                    .withClientAuthorizerDisabled()
                    .withServerAuthorizerDisabled().build());

    @Override
    public void configure(DataConfigurationState dataConfigurationState) {
        innerConfiguration.configure(dataConfigurationState);

        // Disable CloudAuth warning when the project start up
        dataConfigurationState.bind(CloudAuthResourceServerCredentials.KEY,
                (StaticGlobalDataSourceDescriptor<Optional<CloudAuthCredentials>>) (key) -> Optional.empty());
        dataConfigurationState.bind(CloudAuthClientCredentials.KEY,
                (StaticGlobalDataSourceDescriptor<Optional<CloudAuthCredentials>>) (key) -> Optional.empty());
        dataConfigurationState.bind(CloudAuthResourceServerBootstrapper.KEY,
                (StaticGlobalDataSourceDescriptor<Optional<Bootstrapper>>) (key) -> Optional.empty());
        dataConfigurationState.bind(CloudAuthResourceMapper.KEY,
                (StaticGlobalDataSourceDescriptor<Optional<Bootstrapper>>) (key) -> Optional.empty());

    }
}


//CSRF requires a name and the serial for an Odin material set. By default this template has place holders in
//the file /configuration/system-properties/008CsrfKey.properties, there is no requirement to use these default
//property keys, or even to use system properties to supply these values. The CSRF Sevice ID can be varied as
//well, but will be used in tracking metrics so it must meet those requirements.
//You can find more information on the Horizonte wiki, https://w.amazon.com/bin/view/Horizonte/
//        CsrfDataConfiguration.newGlobalStaticMapBuilder()
//                        .addOrReplace(CSRF_SERVICE_ID,
//                                OdinCsrfImplementationsFactory.defaultOx83TokenServiceImpl(
//                                        System.getProperty("amazon.csrf.material.set.name.0x83"),
//                                        Integer.getInteger("amazon.csrf.material.set.serial.0x83")).get())
//                        .withDefaultId(CSRF_SERVICE_ID).build().configure(dataConfigurationState);
//
//
//        CloudAuthDataSourceConfiguration.builder()
//            .withCloudAuthAllCredentialsViaAAA()
//            .build().configure(dataConfigurationState);

//Configure additional DataSource overrides below
