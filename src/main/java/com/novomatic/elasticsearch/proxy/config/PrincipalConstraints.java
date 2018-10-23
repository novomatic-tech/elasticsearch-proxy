package com.novomatic.elasticsearch.proxy.config;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.Set;

@Data
@NoArgsConstructor
public class PrincipalConstraints {
    public static final PrincipalConstraints ANY = new PrincipalConstraints();
    private Set<String> roles = Collections.emptySet();
}
