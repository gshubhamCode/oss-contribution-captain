package org.fa.oss.contribution.helper.service;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;

public class TokenCounter {
    private static final Encoding encoding = Encodings.newDefaultEncodingRegistry().getEncoding("cl100k_base").get();

    public static int countTokens(String input) {
    if (encoding == null) {
        throw new RuntimeException("Token counter not initialized");
        }
        return encoding.encode(input).size();
    }

    public static boolean isWithinLimit(String input, int maxTokens) {
        return countTokens(input) <= maxTokens;
    }
}
