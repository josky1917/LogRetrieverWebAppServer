package com.amazon.aws.vpn.telemetry.horizonte.webapp.config;

import com.amazon.reacttoolkit.injector.configuration.ReactToolkitSpringInterceptorConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Root spring configuration at Application level.
 */
@Configuration
@ComponentScan("com.amazon.aws.vpn.telemetry.horizonte.webapp.controller")
@Import({
        ReactToolkitSpringInterceptorConfiguration.class,
        CustomReactToolkitConfiguration.class
})
public class CustomRootConfig {
}
