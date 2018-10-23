package com.novomatic.elasticsearch.proxy;

import com.novomatic.elasticsearch.proxy.config.AuthorizationRule;
import com.novomatic.elasticsearch.proxy.config.PrincipalConstraints;
import lombok.Getter;
import org.keycloak.representations.AccessToken;

@Getter
public final class Principal {

    private final AccessToken accessToken;
    public Principal(AccessToken accessToken) {
        this.accessToken = accessToken;
    }

    public boolean fulfills(AuthorizationRule authorizationRule) {
        PrincipalConstraints principalConstraints = authorizationRule.getPrincipal();
        if (principalConstraints.getRoles() == null || principalConstraints.getRoles().isEmpty()) {
            return true;
        }
        return principalConstraints.getRoles().stream().anyMatch(role -> {
            String[] tokens = role.split("\\.");
            if (tokens.length >= 2) {
                return accessToken.getResourceAccess(tokens[0]).isUserInRole(tokens[1]);
            } else {
                return accessToken.getRealmAccess().isUserInRole(tokens[0]);
            }
        });
    }

    @Override
    public String toString() {
        return accessToken.getName();
    }
}
