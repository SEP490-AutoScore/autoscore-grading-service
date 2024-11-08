package com.CodeEvalCrew.AutoScore.services.plagiarism_check_service;

import java.sql.Time;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.CodeEvalCrew.AutoScore.models.DTO.StudentSourceInfoDTO;
import com.CodeEvalCrew.AutoScore.models.Entity.NGram;
import com.CodeEvalCrew.AutoScore.models.Entity.Score;
import com.CodeEvalCrew.AutoScore.models.Entity.Source_Detail;
import com.CodeEvalCrew.AutoScore.repositories.score_repository.ScoreRepository;
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
    private final ScoreRepository scoreRepository;
    private final Set<String> fingerprintDatabase = new HashSet<>();
    private final List<CompilationUnit> studentASTList = new ArrayList<>();

    public PlagiarismDetectionService(SourceDetailRepository sourceDetailRepository, ASTComparator astComparator,
            CodeNormalizer codeNormalizer, NGramGenerator nGramGenerator, FingerprintGenerator fingerprintGenerator,
            ScoreRepository scoreRepository, ScoreManager scoreManager) {
        this.sourceDetailRepository = sourceDetailRepository;
        this.astComparator = astComparator;
        this.codeNormalizer = codeNormalizer;
        this.nGramGenerator = nGramGenerator;
        this.fingerprintGenerator = fingerprintGenerator;
        this.scoreRepository = scoreRepository;
        this.scoreManager = scoreManager;
    }

    @Override
    public void runPlagiarismDetection(List<StudentSourceInfoDTO> sourceDetailsDTO) {
        System.out.println("Starting plagiarism detection for " + sourceDetailsDTO.size() + " source details.");
        fingerprintDatabase.clear();
        studentASTList.clear();
        Time start = new Time(System.currentTimeMillis());
        // Giai đoạn 1: Chuẩn hóa và N-grams
        List<Source_Detail> phase1PassedStudents = runNormalizationAndNGrams(sourceDetailsDTO);
        System.out.println("Phase 1 completed. Number of students passed: " + phase1PassedStudents.size());

        // // Giai đoạn 2: So sánh N-grams
        // List<Source_Detail> phase2PassedStudents = runNGramsComparison(phase1PassedStudents);
        // System.out.println("Phase 2 completed. Number of students passed: " + phase2PassedStudents.size());

        // // Giai đoạn 3: Fingerprinting và LSH
        // List<Source_Detail> phase3PassedStudents = runFingerprintingAndLSH(phase2PassedStudents);
        // System.out.println("Phase 3 completed. Number of students passed: " + phase3PassedStudents.size());

        // // Giai đoạn 4: So sánh AST toàn diện
        // runASTComparison(phase3PassedStudents);

        System.err.println("Complete plagiarism detection: " + (new Time(System.currentTimeMillis()).getTime() - start.getTime()) + " ms");
    }

    private List<Source_Detail> runNormalizationAndNGrams(List<StudentSourceInfoDTO> sourceDetailsDTO) {
        List<Source_Detail> passedStudents = new ArrayList<>();
        System.out.println("Starting normalization and N-grams generation for " + sourceDetailsDTO.size() + " source details.");

        for (StudentSourceInfoDTO detailDTO : sourceDetailsDTO) {
            Source_Detail detail = sourceDetailRepository.findById(detailDTO.getSourceDetailId()).get();
            if (detail == null) {
                System.err.println("Source detail not found for ID: " + detailDTO.getSourceDetailId());
                continue; // Skip this iteration
            }

            String normalizedCode = codeNormalizer.normalizeCode(detail.getStudentSourceCodePath());
            System.out.println("Normalized code for student ID: " + detail.getStudent().getStudentId());
            List<String> nGrams = nGramGenerator.generateNGrams(normalizedCode, 5); // 5-grams

            detail.setNormalizedCode(normalizedCode);

            // Xóa nGrams cũ để tránh orphan
            if (detail.getNGrams() != null) {
                detail.getNGrams().clear();
            }

            // Tạo danh sách NGram từ danh sách chuỗi nGrams
            List<NGram> nGramEntities = nGrams.stream()
                    .map(nGramValue -> {
                        NGram nGramEntity = new NGram();
                        nGramEntity.setNGramValue(nGramValue);
                        nGramEntity.setSourceDetail(detail);
                        return nGramEntity;
                    })
                    .collect(Collectors.toList());

            // Thiết lập danh sách NGram vào detail
            detail.getNGrams().addAll(nGramEntities);

            // Thêm detail vào danh sách sinh viên đã qua giai đoạn chuẩn hóa
            passedStudents.add(detail);
        }
        System.out.println("Normalization and N-grams generation completed. Number of students processed: " + passedStudents.size());
        return passedStudents;
    }

    private List<Source_Detail> runNGramsComparison(List<Source_Detail> sourceDetails) {
        List<Source_Detail> passedStudents = new ArrayList<>();
        System.out.println("Starting N-grams comparison for " + sourceDetails.size() + " source details.");

        for (Source_Detail detail : sourceDetails) {
            boolean hasSuspiciousNGram = false;

            // Duyệt qua các NGram của detail
            for (NGram nGramEntity : detail.getNGrams()) {
                String nGram = nGramEntity.getNGramValue();
                if (fingerprintDatabase.contains(nGram)) {
                    hasSuspiciousNGram = true;
                    System.out.println("Suspicious NGram found for student ID: " + detail.getStudent().getStudentId());
                    break;
                }
            }

            if (!hasSuspiciousNGram) {
                passedStudents.add(detail);
            }
        }
        System.out.println("N-grams comparison completed. Number of students passed: " + passedStudents.size());
        return passedStudents;
    }

    private List<Source_Detail> runFingerprintingAndLSH(List<Source_Detail> sourceDetails) {
        List<Source_Detail> passedStudents = new ArrayList<>();
        System.out.println("Starting fingerprinting and LSH for " + sourceDetails.size() + " source details.");

        for (Source_Detail detail : sourceDetails) {
            String fingerprint = fingerprintGenerator.generateFingerprint(detail.getNormalizedCode());
            boolean isSuspicious = fingerprintGenerator.lshCheck(fingerprint);

            if (!isSuspicious) {
                passedStudents.add(detail);
            } else {
                System.out.println("Suspicious fingerprint detected for student ID: " + detail.getStudent().getStudentId());
            }
        }
        System.out.println("Fingerprinting and LSH completed. Number of students passed: " + passedStudents.size());
        return passedStudents;
    }

    private void runASTComparison(List<Source_Detail> sourceDetails) {
        System.out.println("Start comparing AST for " + sourceDetails.size() + " students.");
        astComparator.createASTMatrix(sourceDetails);

        for (int i = 0; i < sourceDetails.size(); i++) {
            Source_Detail detail1 = sourceDetails.get(i);

            for (int j = 0; j < sourceDetails.size(); j++) {
                if (i == j) {
                    continue;  // Bỏ qua so sánh với chính mình
                }
                Source_Detail detail2 = sourceDetails.get(j);

                System.out.println("Comparing AST of student " + detail1.getStudent().getStudentId()
                        + " with student " + detail2.getStudent().getStudentId());

                List<String> similarSections = astComparator.extractSimilarSections(detail1, detail2);

                if (!similarSections.isEmpty()) {
                    // Đánh dấu đạo văn nếu có đoạn mã giống nhau
                    Score score1 = scoreManager.setScoreRecord(detail1, true, similarSections, detail2.getStudent().getStudentCode());
                    Score score2 = scoreManager.setScoreRecord(detail2, true, similarSections, detail1.getStudent().getStudentCode());

                    scoreRepository.save(score1);
                    scoreRepository.save(score2);
                } else {
                    Score score = scoreManager.setScoreRecord(detail1, false, null, null);
                    scoreRepository.save(score);
                }
            }
        }
        System.out.println("Complete AST comparison.");
    }
}