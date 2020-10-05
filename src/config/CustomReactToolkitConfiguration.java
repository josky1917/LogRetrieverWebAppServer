package com.amazon.aws.vpn.telemetry.horizonte.webapp.config;

import amazon.platform.config.AppConfig;
import amazon.platform.tools.ApolloEnvironmentInfo;
import com.amazon.reacttoolkit.injector.ReactToolkitAssetInjector;
import com.amazon.reacttoolkit.injector.interceptor.ReactToolkitAssetsAnnotationHandlerInterceptor;
import com.amazon.reacttoolkit.injector.manifest.ReactToolkitAssetsManifest;
import com.amazon.reacttoolkit.injector.manifest.ReactToolkitManifestException;
import com.google.common.collect.ImmutableMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.WebApplicationContext;

import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Beans for ReactToolkit that map AWS regions PubSub realms to XXAmazon realms(used by front-end dependencies).
 */
@Configuration
public class CustomReactToolkitConfiguration {

    //Override realm attribute in react toolkit asset manifest
    //Now only pdx (Beta) & iad (Gamma) are overridden
    private static final Map<String, String> REALM_MAP = ImmutableMap.of(
            "pdx", "USAmazon", "iad", "USAmazon"
    );

    /**
     * CustomReactToolkitConfiguration.
     */
    @Bean(name = ReactToolkitAssetsManifest.BEAN_NAME)
    ReactToolkitAssetsManifest reactTookitAssetsManifest() throws ReactToolkitManifestException, IOException {
        String reactTookitManifestsPath = Paths.get(new ApolloEnvironmentInfo().getActualRoot(), "lib", "react-toolkit-asset-manifests").toString();
        return new ReactToolkitAssetsManifest(
                reactTookitManifestsPath,
                REALM_MAP.getOrDefault(AppConfig.getRealm().name(), AppConfig.getRealm().name()));
    }

    @Bean(name = ReactToolkitAssetInjector.BEAN_NAME)
    @Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
    ReactToolkitAssetInjector reactTookitAssetInjector(@Named(ReactToolkitAssetsManifest.BEAN_NAME)
                                                               ReactToolkitAssetsManifest reactTookitAssetsManifest) {
        return new ReactToolkitAssetInjector(reactTookitAssetsManifest);
    }

    @Bean(name = ReactToolkitAssetsAnnotationHandlerInterceptor.BEAN_NAME)
    ReactToolkitAssetsAnnotationHandlerInterceptor reactTookitAssetsAnnotationHandlerInterceptor(
            @Named(ReactToolkitAssetInjector.BEAN_NAME) ReactToolkitAssetInjector reactTookitAssetInjector) throws Exception {

        return new ReactToolkitAssetsAnnotationHandlerInterceptor(reactTookitAssetInjector);
    }
}
