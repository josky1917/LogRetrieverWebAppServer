package com.amazon.aws.vpn.telemetry.horizonte.webapp.config;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.springframework.web.WebApplicationInitializer;

import com.amazon.horizonte.initialization.delegate.HorizonteAmazonWebInitializerDelegate;
import com.amazon.spring.platform.runtime.PlatformWebApplicationInitializerBuilder;
import com.amazon.spring.platform.web.application.initializer.api.PlatformWebApplicationInitializer;

import java.util.Arrays;

/**
 * The standard implementation of the {@code PlatformWebApplicationInitializer} interface that the platform
 * uses to initialize web application. This implementation uses as ordered list of delegates for extension
 * points. The potential extension points are described on the {@code WebApplicationInitializerDelegate} interface.
 * This list of delegates is always iterated in reverse order so a particular extension point for the first`
 * delegate will always get called after the same extension point for the delegate next in the list.
 */
public class HorizonteWebApplicationInitializer implements WebApplicationInitializer {

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {

        PlatformWebApplicationInitializer initializer;

        initializer = PlatformWebApplicationInitializerBuilder.getBuilder()
                    .delegate(HorizonteAmazonWebInitializerDelegate.INSTANCE)
                    .delegate(MidwayWebInitializerDelegate.INSTANCE)
                    .additionalRootConfigClass(CustomRootConfig.class)
                    .useDefaultUncaughtExceptionHandler(true)
                    .appConfigOverrides(Arrays.asList("AwsVpnTelemetryHorizonte.cfg"))
                    .build();

        initializer.onStartup(servletContext);
    }
}
