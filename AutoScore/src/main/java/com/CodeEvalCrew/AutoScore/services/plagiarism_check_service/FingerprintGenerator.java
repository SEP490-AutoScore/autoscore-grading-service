package com.CodeEvalCrew.AutoScore.services.plagiarism_check_service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FingerprintGenerator {
    private static final int SEGMENT_LENGTH = 5;  // Độ dài mỗi đoạn
    private static final int THRESHOLD = 3;       // Ngưỡng số đoạn giống nhau để xác định đạo văn
    Set<String> fingerprintDatabase = new HashSet<>();
    
    public FingerprintGenerator(Set<String> fingerprintDatabase) { 
        this.fingerprintDatabase = fingerprintDatabase;
    }

    @Autowired
    private NGramGenerator nGramGenerator;

    @SuppressWarnings("CallToPrintStackTrace")
    String generateFingerprint(String code) {
        try {
            // Tạo N-grams từ mã đã chuẩn hóa
            List<String> nGrams = nGramGenerator.generateNGrams(code, 5);  // Sử dụng 5-grams để tạo fingerprint

            // Kết hợp các N-grams lại để tạo một chuỗi duy nhất
            StringBuilder combined = new StringBuilder();
            for (String nGram : nGrams) {
                combined.append(nGram);
            }

            // Tạo fingerprint bằng SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(combined.toString().getBytes());

            // Chuyển byte array thành chuỗi hex
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            System.out.println("Generated fingerprint for code.");
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            System.err.println("SHA-256 algorithm not found.");
            e.printStackTrace();
            return "";
        }
    }

    boolean lshCheck(String fingerprint) {
        Set<String> segments = new HashSet<>();
        int segmentCount = 0;

        // Chia dấu vân tay thành các phân đoạn và thêm vào `phân đoạn`
        for (int i = 0; i <= fingerprint.length() - SEGMENT_LENGTH; i += SEGMENT_LENGTH) {
            String segment = fingerprint.substring(i, i + SEGMENT_LENGTH);
            segments.add(segment);
            segmentCount++;
        }

        // Tính ngưỡng động dựa trên số lượng phân đoạn (ví dụ: ngưỡng khớp 30%)
        int dynamicThreshold = (int) (segmentCount * 0.3);

        // Điều chỉnh ngưỡng nếu tính toán động thấp hơn ngưỡng đã đặt
        dynamicThreshold = Math.max(dynamicThreshold, THRESHOLD);

        // Đếm các phân đoạn khớp
        int matchCount = 0;
        for (String segment : segments) {
            if (fingerprintDatabase.contains(segment)) {
                matchCount++;
                if (matchCount >= dynamicThreshold) {
                    return true;  // Plagiarism detected
                }
            }
        }

        // Nếu không phát hiện đạo văn => thêm vào cơ sở dữ liệu
        fingerprintDatabase.addAll(segments);
        return false;
    }
}
