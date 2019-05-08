package com.novomatic.elasticsearch.proxy.utils;

import java.util.*;

public class AccessToken implements Token {

    private static final String ISSUER = "iss";
    private static final String SUBJECT = "sub";
    private static final String TYP = "typ";
    private static final String RESOURCE_ACCESS = "resource_access";
    private static final String SCOPES = "scopes";

    private final Map<String, Object> claims;

    private AccessToken(Map<String, Object> claims) {
        this.claims = claims;
    }

    public Map<String, Object> getClaims() {
        return Collections.unmodifiableMap(claims);
    }

    public static class Builder {

        private final Map<String, Object> claims = new HashMap<>();
        private final Map<String, Object> resourceAccess = new HashMap<>();

        public Builder issuer(String issuer) {
            claims.put(ISSUER, issuer);
            return this;
        }

        public Builder subject(String subject) {
            claims.put(SUBJECT, subject);
            return this;
        }

        public Builder type(String type) {
            claims.put(TYP, type);
            return this;
        }

        public Builder resourceAccess(String resourceName, Access access) {
            resourceAccess.put(resourceName, access.toMap());
            claims.put(RESOURCE_ACCESS, resourceAccess);
            return this;
        }

        public Builder scopes(String... scopes){
            claims.put(SCOPES, scopes);
            return this;
        }

        public AccessToken build() {
            return new AccessToken(claims);
        }

    }

    public static class Access {

        private static final String ROLES = "roles";

        private final Set<String> roles;

        public Access(Set<String> roles) {
            this.roles = roles;
        }

        public Access(String... roles) {
            this.roles = new HashSet<>(Arrays.asList(roles));
        }

        public Set<String> getRoles() {
            return this.roles;
        }

        public Map<String, Object> toMap(){
            Map<String, Object> map = new HashMap<>();
            map.put(ROLES, this.roles);
            return map;
        }
    }
}
