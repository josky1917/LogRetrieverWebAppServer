package com.amazon.aws.vpn.telemetry.horizonte.webapp.controller.core;

import com.amazon.horizonte.spring.annotations.PageType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import com.amazon.reacttoolkit.injector.annotation.ReactToolkitAssets;

/**
 * This is a plain Spring MVC controller to handle requests to "/".
 */
@Controller
public class SimpleSpringMVCController {

    /**
     * Home controller method.
     *
     * @return Spring ModelAndView object
     */
    @RequestMapping("**")
    @PageType(pageType = "AppEntry")
    @ReactToolkitAssets(assets = {"AwsVpnTelemetryReactAsset"})
    public ModelAndView execute() {
        return new ModelAndView("home.jsp");
    }
}

