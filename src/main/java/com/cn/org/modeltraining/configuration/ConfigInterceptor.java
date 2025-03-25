package com.cn.org.modeltraining.configuration;


import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class ConfigInterceptor implements HandlerInterceptor {

    @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

            if (request.getMethod().equals(RequestMethod.OPTIONS.name())) {
                return true;
            }else if (request.getMethod().equals(RequestMethod.POST.name())) {
                return true;
            }
            if (request.getRequestURI().equals("/ppt/ppt/getPptData")) {
                return true;
            }
        return true;
    }

}
