package com.novomatic.elasticsearch.proxy.filter;

import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.http.ServletInputStreamWrapper;
import org.springframework.http.HttpStatus;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StreamUtils;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.REQUEST_ENTITY_KEY;

public final class RequestContextExtensions {

    private RequestContextExtensions() {
    }

    public static Optional<String> readRequestBody(RequestContext context) {
        try {
            InputStream in = (InputStream) context.get(REQUEST_ENTITY_KEY);
            if (in == null) {
                in = context.getRequest().getInputStream();
            }
            String body = StreamUtils.copyToString(in, Charset.forName("UTF-8"));
            context.set(REQUEST_ENTITY_KEY, new ByteArrayInputStream(body.getBytes("UTF-8")));
            return Optional.of(body);
        } catch (IOException ex) {
            ReflectionUtils.rethrowRuntimeException(ex);
            return Optional.empty();
        }
    }

    public static String readResponseBody(RequestContext context) {
        try {
            String responseBody = context.getResponseBody();
            if (responseBody == null) {
                InputStream stream = context.getResponseDataStream();
                responseBody = StreamUtils.copyToString(stream, Charset.forName("UTF-8"));
                context.setResponseBody(responseBody);
            }
            return responseBody;
        } catch (IOException e) {
            ReflectionUtils.rethrowRuntimeException(e);
            return null;
        }
    }

    public static void respondWith(RequestContext currentContext, HttpStatus httpStatus) {
        currentContext.unset();
        currentContext.setResponseStatusCode(httpStatus.value());
    }
}
