package com.aivideo.video.provider;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeedanceCallbackSignerTest {

    @Test
    void signsAndVerifiesCallbackUrl() {
        SeedanceVideoProperties properties = new SeedanceVideoProperties();
        properties.setCallbackBaseUrl("https://api.example.com/");
        properties.setCallbackSecret("test-secret");
        SeedanceCallbackSigner signer = new SeedanceCallbackSigner(properties);

        String url = signer.callbackUrl(42L);
        String signature = url.substring(url.lastIndexOf('/') + 1);

        assertTrue(url.startsWith("https://api.example.com/api/v1/video/callbacks/seedance/42/"));
        assertTrue(signer.verify(42L, signature));
        assertFalse(signer.verify(43L, signature));
    }
}
