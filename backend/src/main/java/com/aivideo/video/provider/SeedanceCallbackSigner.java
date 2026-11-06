package com.aivideo.video.provider;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class SeedanceCallbackSigner {

    private final SeedanceVideoProperties properties;

    public SeedanceCallbackSigner(SeedanceVideoProperties properties) {
        this.properties = properties;
    }

    public String callbackUrl(Long localTaskId) {
        String baseUrl = trimTrailingSlash(properties.getCallbackBaseUrl());
        return baseUrl + "/api/v1/video/callbacks/seedance/" + localTaskId
                + "/" + signature(localTaskId);
    }

    public boolean verify(Long localTaskId, String signature) {
        if (signature == null) {
            return false;
        }
        byte[] expected = signature(localTaskId).getBytes(StandardCharsets.US_ASCII);
        byte[] actual = signature.getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expected, actual);
    }

    private String signature(Long localTaskId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    properties.getCallbackSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"));
            return HexFormat.of().formatHex(
                    mac.doFinal(String.valueOf(localTaskId).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("无法生成 Seedance 回调签名", ex);
        }
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
