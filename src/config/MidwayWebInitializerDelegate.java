package com.amazon.aws.vpn.telemetry.horizonte.webapp.config;

import amazon.platform.config.AppConfig;
import com.amazon.aws.vpn.telemetry.horizonte.webapp.controller.core.CoralClient.PinCustomerAccountToInstanceSizeController;
import com.amazon.midway.filter.MidwayProxyDelegationFilter;
import com.amazon.spring.platform.web.application.initializer.api.Constants;
import com.amazon.spring.platform.web.application.initializer.spi.WebApplicationInitializerDelegate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import java.util.EnumSet;

/**
 * MidwayIntegration.
 */
public class MidwayWebInitializerDelegate implements WebApplicationInitializerDelegate {
    /**
     * Instance.
     */
    public static final MidwayWebInitializerDelegate INSTANCE = new MidwayWebInitializerDelegate();
    private static final String FILTER_NAME = "MidwayAuthenticationFilter";
    private static final String PING_ENDPOINT_PATTERN = "/(ping|sping)";
    private static final Logger LOGGER = LogManager.getLogger(PinCustomerAccountToInstanceSizeController.class);

    @Override
    public void addFilters(ServletContext servletContext, WebApplicationContext rootContext, WebApplicationContext dispatcherContext) {
        if (AppConfig.findBoolean("AwsVpnTelemetryHorizonte.use_midway")) {
            LOGGER.info("Using Midway delegate.");
            FilterRegistration.Dynamic filter = servletContext.addFilter(FILTER_NAME, MidwayProxyDelegationFilter.class);
            filter.setInitParameter(MidwayProxyDelegationFilter.SKIP_AUTH_CONFIG_PARAMETER_KEY, PING_ENDPOINT_PATTERN);
            filter.addMappingForServletNames(EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD), false, Constants.DISPATCHER_SERVLET_NAME);
            LOGGER.info("Midway delegate done.");
        }

    }
}
