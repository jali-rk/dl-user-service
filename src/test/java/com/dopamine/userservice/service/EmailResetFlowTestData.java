package com.dopamine.userservice.service;

import java.util.UUID;

/**
 * Small helper for parsing the {tokenId}.{secret} token format used in reset flows.
 */
final class EmailResetFlowTestData {

    private EmailResetFlowTestData() {
    }

    static UUID tokenId(String combinedToken) {
        String[] parts = combinedToken.split("\\.", 2);
        return UUID.fromString(parts[0]);
    }

    static String secret(String combinedToken) {
        String[] parts = combinedToken.split("\\.", 2);
        return parts[1];
    }
}

