package com.CodeEvalCrew.AutoScore.services.plagiarism_check_service;

import java.io.IOException;
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
import com.github.javaparser.ast.CompilationUnit;

@Service
public class PlagiarismDetectionService implements IPlagiarismDetectionService {
    private final SourceDetailRepository sourceDetailRepository;
    private final ASTComparator astComparator;
    private final CodeNormalizer codeNormalizer;
    private final NGramGenerator nGramGenerator;
    private final FingerprintGenerator fingerprintGenerator;
    private final ScoreManager scoreManager;
    private final Set<String> fingerprintDatabase = new HashSet<>();
    private final List<CompilationUnit> studentASTList = new ArrayList<>();
    private static final int THRESHOLD_LOW = 60;
    private static final int THRESHOLD_HIGH = 90;
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    public PlagiarismDetectionService(SourceDetailRepository sourceDetailRepository, ASTComparator astComparator,
            CodeNormalizer codeNormalizer, NGramGenerator nGramGenerator, FingerprintGenerator fingerprintGenerator, ScoreManager scoreManager) {
        this.sourceDetailRepository = sourceDetailRepository;
        this.astComparator = astComparator;
        this.codeNormalizer = codeNormalizer;
        this.nGramGenerator = nGramGenerator;
        this.fingerprintGenerator = fingerprintGenerator;
        this.scoreManager = scoreManager;
    }

    @Override
    @SuppressWarnings("CallToPrintStackTrace")
    public void runPlagiarismDetection(List<StudentSourceInfoDTO> sourceDetailsDTO, String examType, Long organizationId) {
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

            for (Future<?> future : futures) {
                try {
                    future.get(1, TimeUnit.MINUTES); // Thời gian chờ tối đa là 1 phút
                } catch (TimeoutException e) {
                    System.err.println("A task took too long to complete and was cancelled.");
                    future.cancel(true); // Hủy bỏ tác vụ nếu quá thời gian chờ
                } catch (ExecutionException e) {
                    System.err.println("Task encountered an ExecutionException: " + e.getMessage());
                    e.getCause().printStackTrace();
                }
            }

            System.out.println("Running fingerprinting and LSH for all students.");
            List<Future<?>> futuresFingerprint = new ArrayList<>();

            for (Source_Detail studentDetail : studentDetails) {
                Future<?> futureFingerprint = executorService.submit(() -> runFingerprintingAndLSH(studentDetail, dbSourceDetails));
                futuresFingerprint.add(futureFingerprint);
            }

            for (Future<?> future : futuresFingerprint) {
                try {
                    future.get(1, TimeUnit.MINUTES); // Thời gian chờ tối đa là 1 phút
                } catch (TimeoutException e) {
                    System.err.println("A task took too long to complete and was cancelled.");
                    future.cancel(true); // Hủy bỏ tác vụ nếu quá thời gian chờ
                } catch (ExecutionException e) {
                    System.err.println("Task encountered an ExecutionException: " + e.getMessage());
                    e.getCause().printStackTrace();
                }
            }

            executorService.shutdown();
            System.gc();
            System.out.println("Plagiarism detection completed for all students.");
        } catch (InterruptedException e) {
            e.printStackTrace();
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
        double overlapPercentage = 0;
        for (Source_Detail dbDetail : dbSourceDetails) {
            if (studentDetail.getStudent().getStudentId().equals(dbDetail.getStudent().getStudentId())) {
                continue;
            }

            Set<String> studentNGramsSet = new HashSet<>(nGrams);
            Set<String> dbNGramsSet = dbDetail.getNGrams().stream().map(NGram::getNGramValue).collect(Collectors.toSet());

            int totalNGrams = studentNGramsSet.size();
            studentNGramsSet.retainAll(dbNGramsSet);
            int matchingNGrams = studentNGramsSet.size();

            overlapPercentage = (double) matchingNGrams / totalNGrams * 100;

            if (overlapPercentage >= THRESHOLD_LOW) {
                isSuspicious = overlapPercentage >= THRESHOLD_HIGH ? "Definitely Plagiarized" : "Possibly Plagiarized";
                List<String> plagiarizedSegments = new CopyOnWriteArrayList<>(studentNGramsSet);
                CodePlagiarismResult plagiarizedCodeResult = extractFullPlagiarizedCode(
                        normalizedCode, plagiarizedSegments, studentDetail, dbDetail);
                codePlagiarismResults.add(plagiarizedCodeResult);
            }
        }

        // Gọi saveScoreRecord để lưu tất cả các kết quả đạo văn
        scoreManager.saveScoreRecord(studentDetail,
                !isSuspicious.isEmpty() ? isSuspicious : null,
                codePlagiarismResults,
                overlapPercentage);
    }

    private void runFingerprintingAndLSH(Source_Detail studentDetail, List<Source_Detail> dbSourceDetails) {
        String normalizedCode = studentDetail.getNormalizedCode();
        String fingerprint = fingerprintGenerator.generateFingerprint(normalizedCode);
        List<String> segments = new CopyOnWriteArrayList<>(fingerprintGenerator.generateSegments(normalizedCode));

        List<CodePlagiarismResult> codePlagiarismResults = new CopyOnWriteArrayList<>();
        String isSuspicious = "";
        double matchPercentage = 0;
        for (Source_Detail dbDetail : dbSourceDetails) {
            if (studentDetail.getStudent().getStudentId().equals(dbDetail.getStudent().getStudentId())) {
                continue;
            }

            LSHCheckResult checkResult = fingerprintGenerator.lshCheck(fingerprint, studentDetail.getStudent().getStudentCode(), segments);

            if (checkResult.getIsSuspicious() != null && !checkResult.getMatchingSegments().isEmpty()) {
                isSuspicious = checkResult.getIsSuspicious();
                matchPercentage = checkResult.getMatchPercentage();
                CodePlagiarismResult plagiarizedCodeResult = extractFullPlagiarizedCode(
                        normalizedCode, checkResult.getMatchingSegments(), studentDetail, dbDetail);

                if (plagiarizedCodeResult.getSelfCode() != null && !plagiarizedCodeResult.getSelfCode().isEmpty()
                        && plagiarizedCodeResult.getStudentPlagiarism() != null && !plagiarizedCodeResult.getStudentPlagiarism().isEmpty()) {
                    codePlagiarismResults.add(plagiarizedCodeResult);
                }
            }
        }

        // Gọi saveScoreRecord để lưu tất cả các kết quả đạo văn
        scoreManager.saveScoreRecord(studentDetail,
                !isSuspicious.isEmpty() ? isSuspicious : null,
                codePlagiarismResults,
                matchPercentage);
    }

    // private void runASTComparison(Source_Detail studentDetail, List<Source_Detail> dbSourceDetails) {
    //     for (Source_Detail dbDetail : dbSourceDetails) {
    //         if (studentDetail.getStudent().getStudentId().equals(dbDetail.getStudent().getStudentId())) {
    //             continue; // Bỏ qua kiểm tra với chính sinh viên này
    //         }
    //         List<String> similarSections = astComparator.extractSimilarSections(studentDetail, dbDetail);
    //         if (!similarSections.isEmpty()) {
    //             Score score = scoreManager.setScoreRecord(studentDetail, true, similarSections, dbDetail.getStudent().getStudentCode());
    //             scoreRepository.save(score);
    //         }
    //     }
    // }
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

        try {
            Files.walk(Paths.get(sourcePath))
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".cs"))
                    .parallel() // Xử lý song song
                    .forEach(path -> {
                        try {
                            String fileContent = new String(Files.readAllBytes(path));
                            for (String block : studentCodeBlocks) {
                                if (fileContent.contains(block)) {
                                    synchronized (sourceCode) {
                                        sourceCode.append(block).append("\n");
                                    }
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sourceCode;
    }
}
