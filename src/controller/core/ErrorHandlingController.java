package com.amazon.aws.vpn.telemetry.horizonte.webapp.controller.core;

import com.amazon.environment.platform.api.request.IsInternal;
import com.amazon.horizonte.spring.annotations.PageType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
* This class is for error handling.
* Replace the messages and error.jsp file with content you want to show to your users.
* Error handling in Horizonte: https://w.amazon.com/bin/view/Horizonte/Dive_Deeper/ExceptionHandling
* The error messages will only be displayed for internal requests.
*/
@Controller
public class ErrorHandlingController {

    private static final String URL_PAGE_NOT_FOUND = "/404";
    private static final String URL_INTERNAL_SERVER_ERROR = "/500";

    private static final String ERROR_PAGE = "error.jsp";

    private static final String TITLE = "title";
    private static final String MESSAGE = "message";
    private static final String LINK = "link";
    private static final String ERROR_HEADER = "Oops,  something has gone wrong.";
    private static final String ERROR_404 = "Error 404. The page handler is not found.";
    private static final String ERROR_500 = "Error 500. An unexpected error has occurred.";
    private static final String DOCUMENTATION_LINK =
        "<a href=\"https://w.amazon.com/bin/view/Horizonte/Dive_Deeper/ExceptionHandling\">"
        + "Documentation about Horizonte\'s error handling</a>";

    /**
     * No page handler found.
     * @return ModelAndView
    */
    @RequestMapping(URL_PAGE_NOT_FOUND)
    @PageType(pageType = "Error404")
    public ModelAndView noHandlerFound() {
        return generateView(ERROR_404);
    }

    /**
     * Internal server error.
     * @return ModelAndView
     */
    @RequestMapping(URL_INTERNAL_SERVER_ERROR)
    @PageType(pageType = "Error500")
    public ModelAndView internalError() {
        return generateView(ERROR_500);
    }

    /**
     * This is a helper function that generates the required ModelAndView.
     * @return ModelAndView
     */
    private ModelAndView generateView(String errorCode) {
        ModelAndView mav = new ModelAndView();
        mav.setViewName(ERROR_PAGE);
        if (IsInternal.resolveCurrent()) {
            mav.addObject(LINK, DOCUMENTATION_LINK);
            mav.addObject(TITLE, ERROR_HEADER);
            mav.addObject(MESSAGE, errorCode);
        }
        return mav;
    }
}

