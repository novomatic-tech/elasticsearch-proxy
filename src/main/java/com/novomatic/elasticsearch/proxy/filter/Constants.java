package com.novomatic.elasticsearch.proxy.filter;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_DECORATION_FILTER_ORDER;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SIMPLE_HOST_ROUTING_FILTER_ORDER;

public class Constants {

    public static final String PASS_THROUGH = "passThrough";
    public static final String AUTHORIZATION = "authorization";


    public static final int AUTHENTICATION_FILTER_ORDER = PRE_DECORATION_FILTER_ORDER + 10;
    public static final int PRE_AUTHORIZATION_FILTER_ORDER = PRE_DECORATION_FILTER_ORDER + 20;
    public static final int AUTHORIZATION_FILTER_ORDER = PRE_DECORATION_FILTER_ORDER + 30;
    public static final int RESPONSE_PROCESSING_FILTER = SIMPLE_HOST_ROUTING_FILTER_ORDER + 1;

}
