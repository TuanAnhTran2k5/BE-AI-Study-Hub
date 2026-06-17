package AiStudyHub.BE.utils;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

@Component
public class SimHashUtil {

    private static final int HASH_BITS = 64;

    
    // Calculates a 64-bit SimHash for the given text.
    // Returns the hash as a String representing the 64-bit binary, 
    // or could return a Long. We will return String for easier DB storage and debugging.
     
    public String calculateSimHash(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        // 1. Tokenize and calculate word frequencies (weights)
        Map<String, Integer> wordFrequencies = new HashMap<>();
        StringTokenizer tokenizer = new StringTokenizer(text.toLowerCase(), " \t\n\r\f.,;:!?'\"()[]{}<>-_+*/=&#@~`$%^\\|");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (token.length() > 2) { // Ignore very short words
                wordFrequencies.put(token, wordFrequencies.getOrDefault(token, 0) + 1);
            }
        }

        // 2. Initialize vector V
        int[] v = new int[HASH_BITS];

        // 3. Process each word
        for (Map.Entry<String, Integer> entry : wordFrequencies.entrySet()) {
            String word = entry.getKey();
            int weight = entry.getValue();

            long wordHash = hash(word);

            for (int i = 0; i < HASH_BITS; i++) {
                long bitmask = 1L << i;
                if ((wordHash & bitmask) != 0) {
                    v[i] += weight;
                } else {
                    v[i] -= weight;
                }
            }
        }

        // 4. Build final SimHash string
        StringBuilder simHashBuilder = new StringBuilder();
        for (int i = HASH_BITS - 1; i >= 0; i--) {
            if (v[i] > 0) {
                simHashBuilder.append("1");
            } else {
                simHashBuilder.append("0");
            }
        }

        return simHashBuilder.toString();
    }

    
    // Calculates the Hamming distance between two SimHash strings.
    // A distance of <= 3 typically indicates >= 90% similarity.
    public int calculateHammingDistance(String simHash1, String simHash2) {
        if (simHash1 == null || simHash2 == null || simHash1.length() != HASH_BITS || simHash2.length() != HASH_BITS) {
            return HASH_BITS; // Max distance
        }

        int distance = 0;
        for (int i = 0; i < HASH_BITS; i++) {
            if (simHash1.charAt(i) != simHash2.charAt(i)) {
                distance++;
            }
        }
        return distance;
    }

    
    // Helper method to generate a 64-bit hash for a single word.
    // Using a simple FNV-1a like hashing or just String.hashCode() extended.
     
    private long hash(String source) {
        if (source == null || source.isEmpty()) {
            return 0;
        } else {
            long hash = 0xCBF29CE484222325L;
            for (int i = 0; i < source.length(); i++) {
                hash ^= source.charAt(i);
                hash *= 0x100000001B3L;
            }
            return hash;
        }
    }
}
