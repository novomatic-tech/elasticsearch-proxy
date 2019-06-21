package com.novomatic.elasticsearch.proxy.utils;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64;
import net.minidev.json.JSONObject;

public class TokenAuthenticationService {

    private final RSAKey rsaKey;

    public TokenAuthenticationService(RSAKey rsaKey) {
        this.rsaKey = rsaKey;
    }

    public String serializeToken(Token token) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.putAll(token.getClaims());

        Payload payload = new Payload(jsonObject);

        JWSObject jwsObject = new JWSObject(createHeader(), payload);

        try {
            RSASSASigner rsa = new RSASSASigner(rsaKey.toPrivateKey());
            jwsObject.sign(rsa);
            return jwsObject.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    public String getPublicKey() {
        try {
            Base64 publicKey = Base64.encode(rsaKey.toPublicKey().getEncoded());
            return publicKey.toString();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    private JWSHeader createHeader() {
        return new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(JOSEObjectType.JWT)
                .build();
    }
}
