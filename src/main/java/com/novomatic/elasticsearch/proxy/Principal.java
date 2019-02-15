package com.novomatic.elasticsearch.proxy;

import com.novomatic.elasticsearch.proxy.config.AuthorizationRule;
import com.novomatic.elasticsearch.proxy.config.PrincipalConstraints;
import lombok.Getter;
import org.keycloak.representations.AccessToken;

import java.util.HashMap;
import java.util.Map;

@Getter
public final class Principal {

    private final AccessToken token;
    private final Map<String, Object> attributes;

    public Principal(AccessToken accessToken) {
        this.token = accessToken;
        this.attributes = new HashMap<>(accessToken.getOtherClaims());
    }

    public boolean hasRealmRole(String roleName) {
        AccessToken.Access realmAccess = token.getRealmAccess();
        return realmAccess != null && realmAccess.isUserInRole(roleName);
    }

    public boolean hasClientRole(String clientId, String roleName) {
        AccessToken.Access resourceAccess = token.getResourceAccess(clientId);
        return resourceAccess != null && resourceAccess.isUserInRole(roleName);
    }

    public boolean fulfills(PrincipalConstraints principalConstraints) {
        if (principalConstraints.getRoles() == null || principalConstraints.getRoles().isEmpty()) {
            return true;
        }
        return principalConstraints.getRoles().stream().anyMatch(role -> {
            String[] tokens = role.split("\\."); // TODO: refactor to indexOf('.')
            AccessToken.Access access;
            String roleName;
            if (tokens.length >= 2) {
                access = token.getResourceAccess(tokens[0]);
                roleName = tokens[1];
            } else {
                access = token.getRealmAccess();
                roleName = tokens[0];
            }
            return access != null && access.isUserInRole(roleName);
        });
    }

    @Override
    public String toString() {
        return token.getName();
    }
}
