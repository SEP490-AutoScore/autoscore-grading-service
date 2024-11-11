package com.CodeEvalCrew.AutoScore.services.plagiarism_check_service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
    private static final int THRESHOLD_HIGH = 80;

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

    private void runNormalizationAndNGramsComparison(Source_Detail studentDetail, List<Source_Detail> dbSourceDetails) {
        // Bước 1: Chuẩn hóa mã nguồn của sinh viên và tạo N-grams
        String normalizedCode = codeNormalizer.normalizeCode(studentDetail.getStudentSourceCodePath());
        List<String> nGrams = nGramGenerator.generateNGrams(normalizedCode, 5);

        // Lưu lại mã đã chuẩn hóa và danh sách N-grams
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

        // Bước 2: So sánh với từng sinh viên trong cơ sở dữ liệu để tìm các đoạn mã đạo văn
        for (Source_Detail dbDetail : dbSourceDetails) {
            System.out.println("Phase 1: Comparing " + studentDetail.getStudent().getStudentId() + " with " + dbDetail.getStudent().getStudentId());

            if (studentDetail.getStudent().getStudentId().equals(dbDetail.getStudent().getStudentId())) {
                continue;
            }

            Set<String> studentNGramsSet = new HashSet<>(nGrams);
            Set<String> dbNGramsSet = dbDetail.getNGrams().stream().map(NGram::getNGramValue).collect(Collectors.toSet());

            int totalNGrams = studentNGramsSet.size();
            studentNGramsSet.retainAll(dbNGramsSet);
            int matchingNGrams = studentNGramsSet.size();

            double overlapPercentage = (double) matchingNGrams / totalNGrams * 100;
            String isSuspicious = null;

            if (overlapPercentage >= THRESHOLD_HIGH) {
                isSuspicious = "Definitely Plagiarized";
            } else if (overlapPercentage >= THRESHOLD_LOW && overlapPercentage < THRESHOLD_HIGH) {
                isSuspicious = "Possibly Plagiarized";
            }

            if (isSuspicious != null) {
                // Gọi hàm extractFullPlagiarizedCode để lấy các đoạn mã đạo văn cụ thể
                List<String> plagiarizedSegments = new ArrayList<>(studentNGramsSet);
                CodePlagiarismResult plagiarizedCodeResult = extractFullPlagiarizedCode(
                        normalizedCode, plagiarizedSegments, studentDetail, dbDetail);

                // Lưu kết quả vào cơ sở dữ liệu thông qua saveScoreRecord
                List<CodePlagiarismResult> codePlagiarismResults = new ArrayList<>();
                codePlagiarismResults.add(plagiarizedCodeResult);
                scoreManager.saveScoreRecord(studentDetail, isSuspicious, codePlagiarismResults, dbDetail.getStudent().getStudentCode(), overlapPercentage);
            } else {
                scoreManager.saveScoreRecord(studentDetail, null, null, null, overlapPercentage);
            }
        }
    }

    private void runFingerprintingAndLSH(Source_Detail studentDetail, List<Source_Detail> dbSourceDetails) {
        String normalizedCode = studentDetail.getNormalizedCode();
        String fingerprint = fingerprintGenerator.generateFingerprint(normalizedCode);
        List<String> segments = fingerprintGenerator.generateSegments(normalizedCode);
        List<CodePlagiarismResult> codePlagiarismResults = new ArrayList<>();
        LSHCheckResult checkResult = null;

        for (Source_Detail dbDetail : dbSourceDetails) {
            if (studentDetail.getStudent().getStudentId().equals(dbDetail.getStudent().getStudentId())) {
                continue; // Bỏ qua kiểm tra với chính sinh viên này
            }

            checkResult = fingerprintGenerator.lshCheck(fingerprint, studentDetail.getStudent().getStudentCode(), segments);

            if (checkResult.getIsSuspicious() != null && !checkResult.getMatchingSegments().isEmpty()) {
                // Chỉ thực hiện khi có các đoạn mã nghi ngờ là đạo văn
                CodePlagiarismResult plagiarizedCodeResult = extractFullPlagiarizedCode(
                        normalizedCode, checkResult.getMatchingSegments(), studentDetail, dbDetail);

                // Chỉ thêm vào nếu có các đoạn mã thực tế để lưu trữ
                if (plagiarizedCodeResult.getSelfCode() != null && !plagiarizedCodeResult.getSelfCode().isEmpty()) {
                    codePlagiarismResults.add(plagiarizedCodeResult);
                }
            }
        }

        // Chỉ lưu nếu có kết quả đạo văn thực sự
        if (!codePlagiarismResults.isEmpty() &&  checkResult.getIsSuspicious() != null) {
            scoreManager.saveScoreRecord(studentDetail, checkResult.getIsSuspicious(), codePlagiarismResults, null, 0);
        } else {
            scoreManager.saveScoreRecord(studentDetail, null, null, null, 0);
        }
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
        List<String> studentCodeBlocks = new ArrayList<>();
        StringBuilder currentBlock = new StringBuilder();
        CodePlagiarismResult result = new CodePlagiarismResult();
        int lastEndIndex = -1;

        // Bước 1: Tạo danh sách các đoạn mã bị nghi ngờ là đạo văn từ `plagiarizedSegments`
        for (String segment : plagiarizedSegments) {
            int index = normalizedCode.indexOf(segment);

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

        // Bước 2: Duyệt qua các tệp của sinh viên và tìm các đoạn mã đạo văn tương ứng
        StringBuilder sourceCode = new StringBuilder();
        try {
            Files.walk(Paths.get(studentSourceDetail.getStudentSourceCodePath()))
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".cs"))
                    .forEach(path -> {
                        try {
                            String fileContent = new String(Files.readAllBytes(path));
                            for (String block : studentCodeBlocks) {
                                if (fileContent.contains(block)) {
                                    sourceCode.append(block).append("\n"); // Chỉ thêm đoạn mã đạo văn tìm thấy
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Bước 3: Duyệt qua các tệp trong cơ sở dữ liệu sinh viên để tìm các đoạn mã đạo văn tương ứng
        StringBuilder dbSourceCode = new StringBuilder();
        try {
            Files.walk(Paths.get(dbStudentSourceDetail.getStudentSourceCodePath()))
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".cs"))
                    .forEach(path -> {
                        try {
                            String fileContent = new String(Files.readAllBytes(path));
                            for (String block : studentCodeBlocks) {
                                if (fileContent.contains(block)) {
                                    dbSourceCode.append(block).append("\n"); // Chỉ thêm đoạn mã đạo văn tìm thấy
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Thiết lập kết quả với các đoạn mã bị đạo văn từ sinh viên và cơ sở dữ liệu
        result.setSelfCode(sourceCode.toString());
        result.setStudentCodePlagiarism(dbStudentSourceDetail.getStudent().getStudentCode());
        result.setStudentPlagiarism(dbSourceCode.toString());

        return result;
    }
}
