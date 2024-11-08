package com.CodeEvalCrew.AutoScore.services.plagiarism_check_service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FingerprintGenerator {

    private static final int SEGMENT_LENGTH = 10;  // Độ dài mỗi đoạn
    private static final int THRESHOLD = 3;       // Ngưỡng số đoạn giống nhau để xác định đạo văn
    // Set<String> fingerprintDatabase = new HashSet<>();
    // Cơ sở dữ liệu vân tay, lưu dấu vân tay dưới dạng Map với khóa là đoạn mã và giá trị là danh sách các bản ghi sinh viên
    private final Map<String, List<FingerprintRecord>> fingerprintDatabase = new HashMap<>();

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

    // Hàm kiểm tra và lưu dấu vân tay, đồng thời lưu vào fingerprintDatabase nếu chưa có
    public LSHCheckResult lshCheck(String fingerprint, String studentId, List<String> segments) {
        List<String> matchingSegments = new ArrayList<>();
        int totalSegments = segments.size();
        int matchCount = 0;

        for (String segment : segments) {
            List<FingerprintRecord> records = fingerprintDatabase.getOrDefault(segment, new ArrayList<>());
            for (FingerprintRecord record : records) {
                if (!record.getStudentId().equals(studentId)) {
                    matchingSegments.add(segment);
                    matchCount++;
                    break; // Một lần trùng là đủ cho mỗi đoạn
                }
            }
            records.add(new FingerprintRecord(studentId, segments));
            fingerprintDatabase.put(segment, records);
        }

        double matchPercentage = (double) matchCount / totalSegments * 100;
        boolean isSuspicious = matchPercentage >= 30.0;

        String otherStudentId = isSuspicious && !matchingSegments.isEmpty()
                ? fingerprintDatabase.get(matchingSegments.get(0)).get(0).getStudentId()
                : null;

        return new LSHCheckResult(isSuspicious, matchingSegments, otherStudentId);
    }

    public List<String> generateSegments(String normalizedCode) {
        List<String> segments = new ArrayList<>();

        // Chia chuỗi `normalizedCode` thành các đoạn có độ dài `SEGMENT_LENGTH`
        for (int i = 0; i <= normalizedCode.length() - SEGMENT_LENGTH; i++) {
            String segment = normalizedCode.substring(i, i + SEGMENT_LENGTH);
            segments.add(segment);
        }

        return segments;
    }

    // Hàm lấy các đoạn mã trùng lặp giữa sinh viên và cơ sở dữ liệu
    public List<String> getMatchingSegments(String fingerprint) {
        List<String> matchingSegments = new ArrayList<>();

        for (String segment : fingerprint.split("(?<=\\G.{" + SEGMENT_LENGTH + "})")) {
            if (fingerprintDatabase.containsKey(segment)) {
                matchingSegments.add(segment);
            }
        }
        return matchingSegments;
    }

    // Hàm tìm ID sinh viên có đoạn mã trùng lặp
    public String getSimilarStudentId(String fingerprint) {
        for (String segment : fingerprint.split("(?<=\\G.{" + SEGMENT_LENGTH + "})")) {
            List<FingerprintRecord> records = fingerprintDatabase.get(segment);
            if (records != null && !records.isEmpty()) {
                return records.get(0).getStudentId(); // Lấy ID sinh viên đầu tiên có dấu vân tay tương tự
            }
        }
        return null; // Không tìm thấy sinh viên tương tự
    }
}
