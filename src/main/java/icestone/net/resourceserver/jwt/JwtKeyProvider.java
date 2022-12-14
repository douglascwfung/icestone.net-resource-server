package icestone.net.resourceserver.jwt;


import java.io.IOException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import icestone.net.resourceserver.jwt.model.exception.JwtInitializationException;
import icestone.net.resourceserver.util.Base64Util;
import icestone.net.resourceserver.util.ResourceUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtKeyProvider {

    private final ResourceUtil resourceUtil;
    private final Base64Util base64Util;

    @Getter
    private PublicKey publicKey;

    @PostConstruct
    public void init() {
        publicKey = readKey(
            "classpath:keys/publickey.crt",
            "PUBLIC",
            this::publicKeySpec,
            this::publicKeyGenerator
        );
    }

    private <T extends Key> T readKey(String resourcePath, String headerSpec, Function<String, EncodedKeySpec> keySpec, BiFunction<KeyFactory, EncodedKeySpec, T> keyGenerator) {
        try {
            String keyString = resourceUtil.asString(resourcePath);
            //TODO you can check the headers and throw an exception here if you want

            keyString = keyString
                .replace("-----BEGIN " + headerSpec + " KEY-----", "")
                .replace("-----END " + headerSpec + " KEY-----", "")
                .replaceAll("\\s+", "");

            return keyGenerator.apply(KeyFactory.getInstance("RSA"), keySpec.apply(keyString));
        } catch(NoSuchAlgorithmException | IOException e) {
            throw new JwtInitializationException(e);
        }
    }

    private EncodedKeySpec publicKeySpec(String data) {
        return new X509EncodedKeySpec(base64Util.decode(data));
    }

    private PublicKey publicKeyGenerator(KeyFactory kf, EncodedKeySpec spec) {
        try {
            return kf.generatePublic(spec);
        } catch(InvalidKeySpecException e) {
            throw new JwtInitializationException(e);
        }
    }
}
