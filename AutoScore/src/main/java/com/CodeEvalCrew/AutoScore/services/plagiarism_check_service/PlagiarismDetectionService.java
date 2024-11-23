package com.CodeEvalCrew.AutoScore.services.plagiarism_check_service;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.CodeEvalCrew.AutoScore.models.DTO.StudentSourceInfoDTO;
import com.CodeEvalCrew.AutoScore.models.Entity.Enum.Exam_Type_Enum;
import com.CodeEvalCrew.AutoScore.models.Entity.NGram;
import com.CodeEvalCrew.AutoScore.models.Entity.Source_Detail;
import com.CodeEvalCrew.AutoScore.repositories.source_repository.SourceDetailRepository;

@Service
public class PlagiarismDetectionService implements IPlagiarismDetectionService {

    private final SourceDetailRepository sourceDetailRepository;
    private final CodeNormalizer codeNormalizer;
    private final NGramGenerator nGramGenerator;
    private final FingerprintGenerator fingerprintGenerator;
    private final ScoreManager scoreManager;
    private final FinalCheckPlagiarismService finalCheckPlagiarismService;
    private final Set<String> fingerprintDatabase = new HashSet<>();
    private static final int THRESHOLD_LOW = 50;
    private static final int THRESHOLD_HIGH = 70;
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    public PlagiarismDetectionService(SourceDetailRepository sourceDetailRepository, CodeNormalizer codeNormalizer,
            NGramGenerator nGramGenerator, FingerprintGenerator fingerprintGenerator, ScoreManager scoreManager,
            FinalCheckPlagiarismService finalCheckPlagiarismService) {
        this.sourceDetailRepository = sourceDetailRepository;
        this.codeNormalizer = codeNormalizer;
        this.nGramGenerator = nGramGenerator;
        this.fingerprintGenerator = fingerprintGenerator;
        this.scoreManager = scoreManager;
        this.finalCheckPlagiarismService = finalCheckPlagiarismService;
    }

    @Override
    @SuppressWarnings("CallToPrintStackTrace")
    public void runPlagiarismDetection(List<StudentSourceInfoDTO> sourceDetailsDTO, String examType, Long organizationId, Long examPaperId) {
        try {
            System.out.println("Starting plagiarism detection for " + sourceDetailsDTO.size() + " source details.");

            Exam_Type_Enum examTypeEnum = Exam_Type_Enum.valueOf(examType);
            List<Long> studentSourceIds = sourceDetailsDTO.stream()
                    .map(StudentSourceInfoDTO::getSourceDetailId)
                    .collect(Collectors.toList());
            Map<Long, Source_Detail> studentDetailsMap = sourceDetailRepository
                    .findAllById(studentSourceIds)
                    .stream()
                    .collect(Collectors.toMap(Source_Detail::getSourceDetailId, detail -> detail));
            List<Source_Detail> dbSourceDetails = sourceDetailRepository.findAllByTypeAndStudentOrganizationOrganizationId(examTypeEnum, organizationId);
            List<Source_Detail> studentDetails = new CopyOnWriteArrayList<>(studentDetailsMap.values());

            // Normalizing and generating N-grams
            System.out.println("Normalizing and generating N-grams for all students.");
            List<Future<?>> futures = new ArrayList<>();
            for (Source_Detail studentDetail : studentDetails) {
                fingerprintDatabase.clear();
                Future<?> future = executorService.submit(() -> {
                    runNormalizationAndNGramsComparison(studentDetail, dbSourceDetails);
                    // Lưu lại studentDetail sau khi cập nhật normalizedCode và NGrams
                    sourceDetailRepository.save(studentDetail);
                });
                futures.add(future);
            }
            waitForFutures(futures, "Normalization and N-Grams");

            // Fingerprinting and LSH
            System.out.println("Running fingerprinting and LSH for all students.");
            List<Future<?>> futuresFingerprint = new ArrayList<>();
            for (Source_Detail studentDetail : studentDetails) {
                Future<?> futureFingerprint = executorService.submit(() -> runFingerprintingAndLSH(studentDetail, dbSourceDetails));
                futuresFingerprint.add(futureFingerprint);
            }
            waitForFutures(futuresFingerprint, "Fingerprinting and LSH");

            // Final check plagiarism
            System.out.println("Running final check plagiarism for all students.");
            finalCheckPlagiarismService.finalCheckPlagiarism(examPaperId);

            executorService.shutdown();
            System.gc();
            System.out.println("Plagiarism detection completed for all students.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("CallToPrintStackTrace")
    private void waitForFutures(List<Future<?>> futures, String stageName) throws InterruptedException {
        for (Future<?> future : futures) {
            try {
                future.get(1, TimeUnit.MINUTES); // Thời gian chờ tối đa là 1 phút
            } catch (TimeoutException e) {
                System.err.println(stageName + " took too long and was cancelled.");
                future.cancel(true); // Hủy bỏ tác vụ nếu quá thời gian chờ
            } catch (ExecutionException e) {
                System.err.println(stageName + " encountered an error: " + e.getMessage());
                e.getCause().printStackTrace();
            }
        }
    }


    private void runNormalizationAndNGramsComparison(Source_Detail studentDetail, List<Source_Detail> dbSourceDetails) {
        String normalizedCode = codeNormalizer.normalizeCode(studentDetail.getStudentSourceCodePath());
        List<String> nGrams = nGramGenerator.generateNGrams(normalizedCode, 5);

        studentDetail.setNormalizedCode(normalizedCode);
        studentDetail.getNGrams().clear();
        List<NGram> nGramEntities = nGrams.stream()
                .map(nGramValue -> {
                    NGram nGramEntity = new NGram();
                    nGramEntity.setNGramValue(nGramValue);
                    nGramEntity.setSourceDetail(studentDetail);
                    return nGramEntity;
                }).collect(Collectors.toList());
        studentDetail.getNGrams().addAll(nGramEntities);

        List<CodePlagiarismResult> codePlagiarismResults = new CopyOnWriteArrayList<>();
        String isSuspicious = "";
        for (Source_Detail dbDetail : dbSourceDetails) {
            if (studentDetail.getStudent().getStudentId().equals(dbDetail.getStudent().getStudentId())) {
                continue;
            }

            Set<String> studentNGramsSet = new HashSet<>(nGrams);
            Set<String> dbNGramsSet = dbDetail.getNGrams().stream().map(NGram::getNGramValue).collect(Collectors.toSet());

            int totalNGrams = studentNGramsSet.size();
            studentNGramsSet.retainAll(dbNGramsSet);
            int matchingNGrams = studentNGramsSet.size();

            double overlapPercentage = (double) matchingNGrams / totalNGrams * 100;

            if (overlapPercentage >= THRESHOLD_LOW) {
                isSuspicious = overlapPercentage >= THRESHOLD_HIGH ? "DEFINITELY" : "POSSIBLY";
                List<String> plagiarizedSegments = new CopyOnWriteArrayList<>(studentNGramsSet);
                if (plagiarizedSegments.isEmpty()) {
                    System.out.println("No plagiarized segments found.");
                }
                CodePlagiarismResult plagiarizedCodeResult = extractFullPlagiarizedCode(
                        normalizedCode, plagiarizedSegments, studentDetail, dbDetail);
                plagiarizedCodeResult.setPlagiarismPercentage(overlapPercentage);
                plagiarizedCodeResult.setType("SHORT CODE");
                codePlagiarismResults.add(plagiarizedCodeResult);
            }
        }

        // Gọi saveScoreRecord để lưu tất cả các kết quả đạo văn
        scoreManager.saveScoreRecord(studentDetail,
                !isSuspicious.isEmpty() ? isSuspicious : null,
                codePlagiarismResults);
    }

    private void runFingerprintingAndLSH(Source_Detail studentDetail, List<Source_Detail> dbSourceDetails) {
        String normalizedCode = studentDetail.getNormalizedCode();
        String fingerprint = fingerprintGenerator.generateFingerprint(normalizedCode);
        List<String> segments = new CopyOnWriteArrayList<>(fingerprintGenerator.generateSegments(normalizedCode));

        List<CodePlagiarismResult> codePlagiarismResults = new CopyOnWriteArrayList<>();
        String isSuspicious = "";
        for (Source_Detail dbDetail : dbSourceDetails) {
            if (studentDetail.getStudent().getStudentId().equals(dbDetail.getStudent().getStudentId())) {
                continue;
            }

            LSHCheckResult checkResult = fingerprintGenerator.lshCheck(fingerprint, studentDetail.getStudent().getStudentCode(), segments);

            if (checkResult.getIsSuspicious() != null && !checkResult.getMatchingSegments().isEmpty()) {
                isSuspicious = checkResult.getIsSuspicious();
                CodePlagiarismResult plagiarizedCodeResult = extractFullPlagiarizedCode(
                        normalizedCode, checkResult.getMatchingSegments(), studentDetail, dbDetail);

                if (plagiarizedCodeResult.getSelfCode() != null && !plagiarizedCodeResult.getSelfCode().isEmpty()
                        && plagiarizedCodeResult.getStudentPlagiarism() != null && !plagiarizedCodeResult.getStudentPlagiarism().isEmpty()) {
                    plagiarizedCodeResult.setPlagiarismPercentage(checkResult.getMatchPercentage());
                    plagiarizedCodeResult.setType("LONG CODE");
                    codePlagiarismResults.add(plagiarizedCodeResult);
                }
            }
        }

        // Gọi saveScoreRecord để lưu tất cả các kết quả đạo văn
        scoreManager.saveScoreRecord(studentDetail,
                !isSuspicious.isEmpty() ? isSuspicious : null,
                codePlagiarismResults);
    }

    @SuppressWarnings("CallToPrintStackTrace")
    private CodePlagiarismResult extractFullPlagiarizedCode(String normalizedCode, List<String> plagiarizedSegments, Source_Detail studentSourceDetail, Source_Detail dbStudentSourceDetail) {
        List<String> studentCodeBlocks = new CopyOnWriteArrayList<>();
        StringBuilder currentBlock = new StringBuilder();
        CodePlagiarismResult result = new CodePlagiarismResult();
        int lastEndIndex = -1;

        Map<String, Integer> segmentPositions = new HashMap<>();
        for (String segment : plagiarizedSegments) {
            segmentPositions.put(segment, normalizedCode.indexOf(segment));
        }

        for (String segment : segmentPositions.keySet()) {
            int index = segmentPositions.get(segment);

            if (index != -1) {
                index = Math.max(0, Math.min(index, normalizedCode.length()));
                lastEndIndex = Math.max(0, Math.min(lastEndIndex, normalizedCode.length()));

                if (currentBlock.length() > 0 && index <= lastEndIndex) {
                    int overlapLength = lastEndIndex - index;
                    if (overlapLength < segment.length()) {
                        currentBlock.append(segment.substring(overlapLength));
                    }
                } else {
                    if (currentBlock.length() > 0) {
                        studentCodeBlocks.add(currentBlock.toString());
                    }
                    currentBlock = new StringBuilder(segment);
                }
                lastEndIndex = Math.min(index + segment.length(), normalizedCode.length());
            }
        }
        if (currentBlock.length() > 0) {
            studentCodeBlocks.add(currentBlock.toString());
        }

        StringBuffer sourceCode = findPlagiarismCode(studentSourceDetail.getStudentSourceCodePath(), studentCodeBlocks);
        StringBuffer dbSourceCode = findPlagiarismCode(dbStudentSourceDetail.getStudentSourceCodePath(), studentCodeBlocks);

        result.setSelfCode(sourceCode.toString());
        result.setStudentCodePlagiarism(dbStudentSourceDetail.getStudent().getStudentCode());
        result.setStudentPlagiarism(dbSourceCode.toString());

        return result;
    }

    @SuppressWarnings("CallToPrintStackTrace")
    private StringBuffer findPlagiarismCode(String sourcePath, List<String> studentCodeBlocks) {
        StringBuffer sourceCode = new StringBuffer();
        Set<String> uniqueCodeSnippets = new HashSet<>(); // Tập hợp để lưu các đoạn mã duy nhất
        int contextLines = 3; // Số dòng ngữ cảnh trước và sau block

        try {
            Files.walk(Paths.get(sourcePath))
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".cs"))
                    .filter(path -> !path.getFileName().toString().toUpperCase().contains("DBCONTEXT.CS")
                    && !path.getFileName().toString().toUpperCase().contains("PROGRAM.CS"))
                    .parallel() // Xử lý song song
                    .forEach(path -> {
                        List<String> fileLines = null;
                        try {
                            // Thử đọc file với UTF-8
                            fileLines = Files.readAllLines(path, StandardCharsets.UTF_8);
                        } catch (MalformedInputException e) {
                            try {
                                // Nếu UTF-8 thất bại, thử với ISO_8859_1
                                fileLines = Files.readAllLines(path, StandardCharsets.ISO_8859_1);
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        // Nếu fileLines đã được đọc thành công
                        if (fileLines != null) {
                            for (String block : studentCodeBlocks) {
                                for (int i = 0; i < fileLines.size(); i++) {
                                    if (fileLines.get(i).contains(block)) {
                                        // Lấy đoạn mã xung quanh block
                                        int start = Math.max(i - contextLines, 0);
                                        int end = Math.min(i + contextLines, fileLines.size() - 1);

                                        // Ghép đoạn mã từ `start` đến `end` thành một chuỗi
                                        StringBuilder snippet = new StringBuilder();
                                        snippet.append("File: ").append(path.getFileName().toString()).append("\n");
                                        for (int j = start; j <= end; j++) {
                                            snippet.append(fileLines.get(j)).append("\n");
                                        }
                                        snippet.append("\n---\n"); // Ngăn cách giữa các đoạn

                                        // Kiểm tra xem đoạn mã có trùng lặp không
                                        synchronized (uniqueCodeSnippets) {
                                            if (uniqueCodeSnippets.add(snippet.toString())) {
                                                // Chỉ thêm vào `sourceCode` nếu đoạn mã là duy nhất
                                                sourceCode.append(snippet);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sourceCode;
    }
}
