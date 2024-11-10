package com.CodeEvalCrew.AutoScore.services.plagiarism_check_service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.CodeEvalCrew.AutoScore.models.DTO.StudentSourceInfoDTO;
import com.CodeEvalCrew.AutoScore.models.Entity.Enum.Exam_Type_Enum;
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
    private static final int THRESHOLD_LOW = 60;
    private static final int THRESHOLD_HIGH = 80;

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
    public void runPlagiarismDetection(List<StudentSourceInfoDTO> sourceDetailsDTO, String examType, Long organizationId) {
        System.out.println("Starting plagiarism detection for " + sourceDetailsDTO.size() + " source details.");

        Exam_Type_Enum examTypeEnum = Exam_Type_Enum.valueOf(examType);
        List<Source_Detail> dbSourceDetails = sourceDetailRepository.findAllByTypeAndStudentOrganizationOrganizationId(examTypeEnum, organizationId);

        // Chuẩn hóa và N-grams
        System.out.println("Normalizing and generating N-grams for all students.");
        for (StudentSourceInfoDTO studentSource : sourceDetailsDTO) {
            Optional<Source_Detail> optionalDetail = sourceDetailRepository.findById(studentSource.getSourceDetailId());

            if (optionalDetail.isPresent()) {
                Source_Detail studentDetail = optionalDetail.get();

                // Làm mới fingerprintDatabase cho sinh viên hiện tại
                fingerprintDatabase.clear();

                runNormalizationAndNGramsComparison(studentDetail, dbSourceDetails);
            }
        }

        // Fingerprinting và LSH, kiểm tra trùng lặp với sinh viên khác
        System.out.println("Running fingerprinting and LSH for all students.");
        for (StudentSourceInfoDTO studentSource : sourceDetailsDTO) {
            Optional<Source_Detail> optionalDetail = sourceDetailRepository.findById(studentSource.getSourceDetailId());

            if (optionalDetail.isPresent()) {
                Source_Detail studentDetail = optionalDetail.get();

                runFingerprintingAndLSH(studentDetail, dbSourceDetails);
            }
        }

        // // Giai đoạn 4 cho tất cả sinh viên nếu cần
        // for (StudentSourceInfoDTO studentSource : sourceDetailsDTO) {
        //     Optional<Source_Detail> optionalDetail = sourceDetailRepository.findById(studentSource.getSourceDetailId());
        //     if (optionalDetail.isPresent()) {
        //         Source_Detail studentDetail = optionalDetail.get();
        //         // Giai đoạn 4: So sánh AST toàn diện
        //         runASTComparison(studentDetail, dbSourceDetails);
        //     }
        // }
        System.out.println("Plagiarism detection completed for all students.");
    }

    private boolean runNormalizationAndNGramsComparison(Source_Detail studentDetail, List<Source_Detail> dbSourceDetails) {
        // Bước 1: Chuẩn hóa mã nguồn của sinh viên và tạo N-grams
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

        // Bước 2: Kiểm tra trùng lặp với các sinh viên khác trong cơ sở dữ liệu
        for (Source_Detail dbDetail : dbSourceDetails) {
            System.out.println("Phase 1: Comparing " + studentDetail.getStudent().getStudentId() + " with " + dbDetail.getStudent().getStudentId());
            // Bỏ qua nếu là cùng sinh viên
            if (studentDetail.getStudent().getStudentId().equals(dbDetail.getStudent().getStudentId())) {
                continue;
            }

            // Tập hợp N-grams của sinh viên hiện tại và sinh viên trong cơ sở dữ liệu
            Set<String> studentNGramsSet = nGrams.stream().collect(Collectors.toSet());
            Set<String> dbNGramsSet = dbDetail.getNGrams().stream().map(NGram::getNGramValue).collect(Collectors.toSet());

            // Kiểm tra trùng lặp bằng cách tính tỷ lệ N-grams trùng lặp
            int totalNGrams = studentNGramsSet.size();
            studentNGramsSet.retainAll(dbNGramsSet); // Giữ lại các N-grams trùng lặp
            int matchingNGrams = studentNGramsSet.size();

            // Tính tỷ lệ trùng lặp (matchingNGrams / totalNGrams) và kiểm tra ngưỡng
            double overlapPercentage = (double) matchingNGrams / totalNGrams * 100;
            String isSuspicious = null;

            if (overlapPercentage >= THRESHOLD_HIGH) {
                isSuspicious = "Definitely Plagiarized";
            } else if (overlapPercentage >= THRESHOLD_LOW && overlapPercentage < THRESHOLD_HIGH) {
                isSuspicious = "Possibly Plagiarized";
            }

            if (isSuspicious != null) {
                // Đánh dấu đạo văn nếu tỷ lệ trùng lặp theo yêu cầu
                Score score = scoreManager.setScoreRecord(studentDetail, isSuspicious, new ArrayList<>(studentNGramsSet), dbDetail.getStudent().getStudentCode(), overlapPercentage);
                scoreRepository.save(score);
                return false; // Phát hiện đạo văn
            }
            Score score = scoreManager.setScoreRecord(studentDetail, null, null, null, overlapPercentage);
            scoreRepository.save(score);
        }
        return true; // Không phát hiện đạo văn
    }

    private boolean runFingerprintingAndLSH(Source_Detail studentDetail, List<Source_Detail> dbSourceDetails) {
        String normalizedCode = studentDetail.getNormalizedCode();
        String fingerprint = fingerprintGenerator.generateFingerprint(normalizedCode);
        List<String> segments = fingerprintGenerator.generateSegments(normalizedCode);

        for (Source_Detail dbDetail : dbSourceDetails) {
            System.out.println("Phase 2: Comparing " + studentDetail.getStudent().getStudentId() + " with " + dbDetail.getStudent().getStudentId());
            // Bỏ qua so sánh với chính sinh viên này
            if (studentDetail.getStudent().getStudentId().equals(dbDetail.getStudent().getStudentId())) {
                continue;
            }

            // Tạo fingerprintDatabase riêng biệt để tránh xung đột
            LSHCheckResult checkResult = fingerprintGenerator.lshCheck(fingerprint, studentDetail.getStudent().getStudentCode(), segments);

            if (checkResult.getIsSuspicious() != null) {
                List<String> plagiarizedSegments = checkResult.getMatchingSegments();
                List<String> plagiarizedCodeBlocks = extractFullPlagiarizedCode(normalizedCode, plagiarizedSegments);
                Score score = scoreManager.setScoreRecord(studentDetail, checkResult.getIsSuspicious(), plagiarizedCodeBlocks, dbDetail.getStudent().getStudentCode(), checkResult.getMatchPercentage());
                scoreRepository.save(score);
                return false; // Phát hiện đạo văn
            } else {
                Score score = scoreManager.setScoreRecord(studentDetail, null, null, null, checkResult.getMatchPercentage());
                scoreRepository.save(score);
            }
        }
        return true; // Không phát hiện đạo văn
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
    private List<String> extractFullPlagiarizedCode(String normalizedCode, List<String> plagiarizedSegments) {
        List<String> codeBlocks = new ArrayList<>();
        StringBuilder currentBlock = new StringBuilder();
        int lastEndIndex = -1;

        for (String segment : plagiarizedSegments) {
            int index = normalizedCode.indexOf(segment);

            if (index != -1) {
                // Đảm bảo index và lastEndIndex nằm trong giới hạn
                index = Math.max(0, Math.min(index, normalizedCode.length()));
                lastEndIndex = Math.max(0, Math.min(lastEndIndex, normalizedCode.length()));

                // Kiểm tra nếu đoạn mã hiện tại liền kề hoặc trùng với đoạn mã trước đó
                if (currentBlock.length() > 0 && index <= lastEndIndex) {
                    int overlapLength = lastEndIndex - index;

                    // Đảm bảo không xảy ra tình trạng chỉ số ngoài giới hạn khi dùng substring
                    if (overlapLength < segment.length()) {
                        currentBlock.append(segment.substring(overlapLength));
                    }
                } else {
                    // Thêm đoạn mã hiện tại vào danh sách nếu có nội dung
                    if (currentBlock.length() > 0) {
                        codeBlocks.add(currentBlock.toString());
                    }
                    // Khởi tạo một đoạn mã mới
                    currentBlock = new StringBuilder(segment);
                }

                // Cập nhật vị trí kết thúc của đoạn mã hiện tại, đảm bảo không vượt quá giới hạn
                lastEndIndex = Math.min(index + segment.length(), normalizedCode.length());
            }
        }

        // Thêm đoạn mã cuối cùng nếu có
        if (currentBlock.length() > 0) {
            codeBlocks.add(currentBlock.toString());
        }

        return codeBlocks;
    }
}
