package com.novomatic.elasticsearch.proxy.filter;

import com.netflix.util.Pair;
import com.netflix.zuul.context.RequestContext;
import org.springframework.http.HttpStatus;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

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
            context.set(REQUEST_ENTITY_KEY, new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
            return Optional.of(body);
        } catch (IOException ex) {
            ReflectionUtils.rethrowRuntimeException(ex);
            return Optional.empty();
        }
    }

    public static String readResponseBody(RequestContext context) {
        String responseBody = context.getResponseBody();
        if (responseBody == null) {
            try {
                InputStream responseDataStream = context.getResponseDataStream();
                Optional<Pair<String, String>> contentEncoding = context.getOriginResponseHeaders().stream()
                        .filter(pair -> pair.first().toLowerCase().equals("content-encoding") && pair.second().toLowerCase().contains("gzip"))
                        .findFirst();
                if (contentEncoding.isPresent()) {
                    responseDataStream = new GZIPInputStream(responseDataStream);
                    context.getZuulResponseHeaders().remove(contentEncoding.get());
                }
                responseBody = StreamUtils.copyToString(responseDataStream, StandardCharsets.UTF_8);
                context.setResponseBody(responseBody);
            } catch (IOException e) {
                ReflectionUtils.rethrowRuntimeException(e);
            }
        }
        return responseBody;
    }

    public static void respondWith(RequestContext currentContext, HttpStatus httpStatus) {
        currentContext.unset();
        currentContext.setResponseStatusCode(httpStatus.value());
    }
}
