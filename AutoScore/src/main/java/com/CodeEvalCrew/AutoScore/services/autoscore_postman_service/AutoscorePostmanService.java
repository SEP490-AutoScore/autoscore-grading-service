package com.CodeEvalCrew.AutoScore.services.autoscore_postman_service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.CodeEvalCrew.AutoScore.controllers.SSEController;
import com.CodeEvalCrew.AutoScore.mappers.SourceDetailMapperforAutoscore;
import com.CodeEvalCrew.AutoScore.models.DTO.ResponseDTO.StudentDeployResult;
import com.CodeEvalCrew.AutoScore.models.DTO.StudentSourceInfoDTO;
import com.CodeEvalCrew.AutoScore.models.Entity.Exam_Database;
import com.CodeEvalCrew.AutoScore.models.Entity.Exam_Paper;
import com.CodeEvalCrew.AutoScore.models.Entity.Exam_Question;
import com.CodeEvalCrew.AutoScore.models.Entity.GradingProcess;
import com.CodeEvalCrew.AutoScore.models.Entity.Postman_For_Grading;
import com.CodeEvalCrew.AutoScore.models.Entity.Score;
import com.CodeEvalCrew.AutoScore.models.Entity.Score_Detail;
import com.CodeEvalCrew.AutoScore.models.Entity.Source_Detail;
import com.CodeEvalCrew.AutoScore.models.Entity.Student;
import com.CodeEvalCrew.AutoScore.repositories.exam_repository.IExamPaperRepository;
import com.CodeEvalCrew.AutoScore.repositories.exam_repository.IExamQuestionRepository;
import com.CodeEvalCrew.AutoScore.repositories.examdatabase_repository.IExamDatabaseRepository;
import com.CodeEvalCrew.AutoScore.repositories.grading_process_repository.GradingProcessRepository;
import com.CodeEvalCrew.AutoScore.repositories.postman_for_grading.PostmanForGradingRepository;
import com.CodeEvalCrew.AutoScore.repositories.score_detail_repository.ScoreDetailRepository;
import com.CodeEvalCrew.AutoScore.repositories.score_repository.ScoreRepository;
import com.CodeEvalCrew.AutoScore.repositories.source_repository.SourceDetailRepository;
import com.CodeEvalCrew.AutoScore.repositories.source_repository.SourceRepository;
import com.CodeEvalCrew.AutoScore.repositories.student_repository.StudentRepository;
import com.CodeEvalCrew.AutoScore.utils.PathUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.Network;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.okhttp.OkHttpDockerCmdExecFactory;

@Service
public class AutoscorePostmanService implements IAutoscorePostmanService {

    // private static final String DB_URL =
    // "jdbc:sqlserver://MSI\\SQLSERVER;databaseName=master;user=sa;password=123456;encrypt=false;trustServerCertificate=true;";
    // private static final String DB_DRIVER =
    // "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    // private static final String DB_SERVER = "192.168.1.223\\SQLSERVER";
    // private static final String DB_UID = "sa";
    // private static final String DB_PWD = "123456";
    // private static final String DOCKER_DESKTOP_PATH = "C:\\Program
    // Files\\Docker\\Docker\\Docker Desktop.exe";
    // private static final String NEWMAN_CMD_PATH =
    // "C:\\Users\\nhatt\\AppData\\Roaming\\npm\\newman.cmd";
    // private static final int BASE_PORT = 10000;
    // String directoryPath = "C:\\Project\\AutoScore\\Grading";
    // private static final String CONFIG_MEMORY_PROCESSOR =
    // "C:\\Users\\Admin\\.wslconfig";
    @Autowired
    private SourceRepository sourceRepository;
    @Autowired
    private GradingProcessRepository gradingProcessRepository;
    @Autowired
    private SourceDetailRepository sourceDetailRepository;
    @Autowired
    private SourceDetailMapperforAutoscore sourceDetailMapper;
    @Autowired
    private IExamDatabaseRepository examDatabaseRepository;
    @Autowired
    private IExamPaperRepository examPaperRepository;
    @Autowired
    private ScoreRepository scoreRepository;
    @Autowired
    private ScoreDetailRepository scoreDetailRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private IExamQuestionRepository examQuestionRepository;
    @Autowired
    private PostmanForGradingRepository postmanForGradingRepository;
    @Autowired
    private SSEController sseController;

    @Override
    public List<StudentSourceInfoDTO> gradingFunction(List<StudentSourceInfoDTO> studentSources,
            Long examPaperId, int numberDeploy, Long memory_Megabyte, Long processors) {

        // Kiểm tra điều kiện `fileCollectionPostman` và `isComfirmFile`
        Optional<Exam_Paper> optionalExamPaper = examPaperRepository.findById(examPaperId);
        if (optionalExamPaper.isEmpty()) {
            System.err.println("Exam Paper không tồn tại.");
            sseController.pushEvent(1L, "Exam Paper không tồn tại.", 0, 10, LocalDateTime.now());
            return null;
        }

        Exam_Paper examPaper = optionalExamPaper.get();
        if (examPaper.getFileCollectionPostman() == null || examPaper.getFileCollectionPostman().length == 0) {
            System.err.println("Exam Paper không có dữ liệu Postman Collection.");
            sseController.pushEvent(1L, "Exam Paper không có dữ liệu Postman Collection.", 0, 10, LocalDateTime.now());
            return null;
        }
//
        if (!Boolean.TRUE.equals(examPaper.getIsComfirmFile())) {
            System.err.println("Exam Paper chưa được xác nhận.");
            sseController.pushEvent(1L, "Exam Paper chưa được xác nhận.", 0, 10, LocalDateTime.now());
            return null;
        }

        // Kiểm tra Exam_Database
        Optional<Exam_Database> optionalExamDatabase = examDatabaseRepository.findById(examPaper.getExamPaperId());
        if (optionalExamDatabase.isEmpty()) {
            System.err.println("Exam Database không tồn tại.");
            sseController.pushEvent(1L, "Exam Database không tồn tại.", 0, 10, LocalDateTime.now());
            return null;
        }

        // Config memory and processor for deploying docker
        try {
            File configFile = new File(PathUtil.CONFIG_MEMORY_PROCESSOR);

            // Check conditions for memory_Megabyte and processors
            if (memory_Megabyte == 0 || processors == 0) {
                // Delete the .wslconfig file if it exists
                if (configFile.exists()) {
                    Files.delete(Path.of(PathUtil.CONFIG_MEMORY_PROCESSOR));
                    System.out.println(".wslconfig file deleted due to zero memory or processors request.");
                }
            } else {
                // Delete the .wslconfig file if it exists and create a new one
                if (configFile.exists()) {
                    Files.delete(Path.of(PathUtil.CONFIG_MEMORY_PROCESSOR));
                }

                try (FileWriter writer = new FileWriter(PathUtil.CONFIG_MEMORY_PROCESSOR)) {
                    writer.write("[wsl2]\n");
                    writer.write("memory=" + memory_Megabyte + "MB\n");
                    writer.write("processors=" + processors + "\n");
                    System.out.println(".wslconfig file created with new configuration.");
                }
            }
        } catch (IOException e) {
            System.err.println("Error managing .wslconfig file: " + e.getMessage());
        }

        // Chạy Postman Collection và lấy kết quả
        String postmanResult = runPostmanCollection(examPaperId);
        if (postmanResult == null) {
            System.err.println("Không thể chạy Postman Collection.");
            sseController.pushEvent(1l, "Cannot run Postman Collection.", 0, 10, LocalDateTime.now());
            return null;
        }

        // So sánh các kết quả postmanFunctionName với cơ sở dữ liệu
        if (!comparePostmanResults(postmanResult, examPaperId)) {
            System.err.println("Kết quả Postman không trùng khớp với dữ liệu trong cơ sở dữ liệu.");
            return null;
        }

        // Kiểm tra xem Docker đã khởi động thành công hay chưa
        if (!startDocker()) {
            System.err.println("Docker không khởi động thành công. Vui lòng kiểm tra Docker Desktop.");
            return null; // Hoặc throw một ngoại lệ để xử lý lỗi
        }

        // List<StudentSourceInfoDTO> studentSources = sourceDetailRepository
        // .findBySource_ExamPaper_ExamPaperIdOrderByStudent_StudentId(examPaperId)
        // .stream()
        // .map(sourceDetail -> sourceDetailMapper.toDTO(sourceDetail))
        // .collect(Collectors.toList());
        deleteAndCreateDatabaseByExamPaperId(examPaperId);
        deleteAllFilesAndFolders(PathUtil.DIRECTORY_PATH);

        processStudentSolutions(studentSources, examPaperId, numberDeploy);

        // Filter for students with totalScore > 0
        List<StudentSourceInfoDTO> studentsWithScores = studentSources.stream()
                .filter(student -> {
                    Score score = scoreRepository.findByStudentIdAndExamPaperId(student.getStudentId(), examPaperId);
                    return score != null && score.getTotalScore() > 0;
                })
                .map(student -> new StudentSourceInfoDTO(
                        student.getSourceDetailId(),
                        student.getStudentId(),
                        student.getStudentSourceCodePath()))
                .collect(Collectors.toList());

        return studentsWithScores;
    }

    public void processStudentSolutions(List<StudentSourceInfoDTO> studentSources, Long examPaperId, int numberDeploy) {
        int totalBatches = (int) Math.ceil((double) studentSources.size() / numberDeploy);

        for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
            int start = batchIndex * numberDeploy;
            int end = Math.min(start + numberDeploy, studentSources.size());
            List<StudentSourceInfoDTO> currentBatch = studentSources.subList(start, end);

            ExecutorService executor = Executors.newFixedThreadPool(currentBatch.size());
            Map<Future<StudentDeployResult>, StudentSourceInfoDTO> futureToStudentSourceMap = new HashMap<>();

            // List successful deployments
            List<StudentSourceInfoDTO> successfulDeployments = new ArrayList<>();

            for (int i = 0; i < currentBatch.size(); i++) {
                StudentSourceInfoDTO studentSource = currentBatch.get(i);
                Path dirPath = Paths.get(studentSource.getStudentSourceCodePath());
                int port = PathUtil.BASE_PORT + i;
                Long studentId = studentSource.getStudentId();

                createFileCollectionPostman(examPaperId, studentSource.getSourceDetailId(), port);

                // Xóa container docker
                try {
                    deleteContainerAndImages();
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException("Failed to delete containers and images: " + e.getMessage());
                }

                Future<StudentDeployResult> future = executor.submit(() -> {

                    try {
                        removeDockerFiles(dirPath);
                        var csprojAndVersion = findCsprojAndDotnetVersion(dirPath);

                        if (csprojAndVersion != null) {
                            createDockerfile(dirPath, csprojAndVersion.getKey(),
                                    csprojAndVersion.getValue(), port);
                            createDockerCompose(dirPath, studentId, port);
                            findAndUpdateAppsettings(dirPath, examPaperId, port);

                        }

                        return deployStudentSolution(studentSource);

                    } catch (IOException e) {
                        e.printStackTrace();
                        return new StudentDeployResult(studentId, false, "IOException: " + e.getMessage());
                    }
                });

                futureToStudentSourceMap.put(future, studentSource);
            }
            executor.shutdown();

            try {
                executor.awaitTermination(60, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Hiển thị kết quả triển khai cho mỗi sinh viên trong nhóm hiện tại
            for (Map.Entry<Future<StudentDeployResult>, StudentSourceInfoDTO> entry : futureToStudentSourceMap
                    .entrySet()) {
                Future<StudentDeployResult> future = entry.getKey();
                StudentSourceInfoDTO studentSource = entry.getValue();

                try {
                    StudentDeployResult result = future.get();
                    System.out.println(result.getMessage() + " for studentId: " + result.getStudentId());

                    if (!result.isSuccessful()) {
                        recordFailure(result.getStudentId(), examPaperId, "Cannot deploy Docker.");
                    } else {
                        successfulDeployments.add(studentSource);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }

            // Run Newman for successful deployments
            if (!successfulDeployments.isEmpty()) {
                for (StudentSourceInfoDTO successfulStudent : successfulDeployments) {

                    // Call getAndRunPostmanCollection and get both the function results and the log
                    Pair<Map<String, Integer>, String> resultAndLog = getAndRunPostmanCollection(
                            successfulStudent.getStudentId(), successfulStudent.getSourceDetailId());

                    Map<String, Integer> passedFunctionNames = resultAndLog.getLeft(); // Function results
                    String logBuilder = resultAndLog.getRight(); // Logs

                    // Convert List to Map to get count of each function's success
                    Map<String, Long> functionPassedCountMap = passedFunctionNames.entrySet().stream()
                            .collect(Collectors.groupingBy(Map.Entry::getKey,
                                    Collectors.summingLong(Map.Entry::getValue)));

                    System.out.println(
                            "Function passed count map for studentId " + successfulStudent.getStudentId() + ": "
                                    + functionPassedCountMap);

                    // Call saveScoreAndScoreDetail with the Map and logBuilder
                    saveScoreAndScoreDetail(successfulStudent.getStudentId(), examPaperId, functionPassedCountMap,
                            logBuilder);
                    deleteAndCreateDatabaseByExamPaperId(examPaperId);
                }
            } else {
                System.out.println("No successful deployments found to run Newman.");
            }

        }
    }

    private Pair<Map<String, Integer>, String> getAndRunPostmanCollection(Long studentId, Long sourceDetailId) {
        Map<String, Integer> functionResults = new HashMap<>();
        String currentFunction = null;
        int passCount = 0;
        StringBuilder logBuilder = new StringBuilder(); // Dùng StringBuilder để lưu log

        try {
            // Tạo thư mục sinh viên nếu chưa có
            Path studentDir = Paths.get(PathUtil.DIRECTORY_PATH, String.valueOf(studentId));
            Files.createDirectories(studentDir);

            // Lấy dữ liệu collection và tạo file Postman
            Source_Detail sourceDetail = sourceDetailRepository.findById(sourceDetailId)
                    .orElseThrow(() -> new RuntimeException("Source_Detail not found with ID: " + sourceDetailId));

            Path postmanFilePath = studentDir.resolve(studentId + ".json");

            byte[] postmanCollection = sourceDetail.getFileCollectionPostman();
            Objects.requireNonNull(postmanCollection,
                    "fileCollectionPostman is null for sourceDetailId: " + sourceDetailId);
            Files.write(postmanFilePath, postmanCollection);

            // Chờ cho file được tạo thành công trước khi tiếp tục
            int waitTimeInSeconds = 10;
            int intervalInMilliseconds = 500;
            int waited = 0;
            while (!Files.exists(postmanFilePath) && waited < waitTimeInSeconds * 1000) {
                Thread.sleep(intervalInMilliseconds);
                waited += intervalInMilliseconds;
            }

            if (!Files.exists(postmanFilePath)) {
                throw new IOException("Failed to create Postman collection file within timeout.");
            }

            System.out.println("Running Newman for studentId: " + studentId);

            // Cấu hình ProcessBuilder
            ProcessBuilder processBuilder = new ProcessBuilder(PathUtil.NEWMAN_CMD_PATH, "run",
                    postmanFilePath.toString());

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Đặt tên file lưu output
            Path outputFile = studentDir.resolve(studentId + ".txt");

            try (
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream(), "UTF-8"));
                    BufferedWriter writer = Files.newBufferedWriter(outputFile)) {

                String line;
                while ((line = reader.readLine()) != null) {
                    writer.write(line);
                    writer.newLine();

                    line = line.trim();
                    logBuilder.append(line).append("\n"); // Lưu log vào StringBuilder

                    if (line.startsWith("→")) {
                        if (currentFunction != null) {
                            functionResults.put(currentFunction, passCount);
                        }

                        currentFunction = line.substring(2).trim(); // Get the function name after "→"
                        passCount = 0;
                    } else if (line.startsWith("√")) {
                        passCount++;
                    }
                }

                if (currentFunction != null) {
                    functionResults.put(currentFunction, passCount);
                }

            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.out.println("Newman execution failed with exit code: " + exitCode);
            } else {
                System.out.println("Newman executed successfully.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Return both function results and the log as a Pair
        return Pair.of(functionResults, logBuilder.toString());
    }

    public void saveScoreAndScoreDetail(Long studentId, Long examPaperId,
            Map<String, Long> functionPassedCountMap, String logBuilder) {
        Student student = studentRepository.findById(studentId).orElse(null);
        Exam_Paper examPaper = examPaperRepository.findById(examPaperId).orElse(null);

        if (student == null || examPaper == null) {
            System.err.println("Student hoặc Exam Paper không tồn tại.");
            return;
        }

        // Sử dụng StringBuilder để lưu lý do
        StringBuilder reasonBuilder = new StringBuilder();

        Score score = new Score();
        score.setStudent(student);
        score.setExamPaper(examPaper);
        score.setGradedAt(LocalDateTime.now());

        score.setLogRunPostman(logBuilder.toString()); // Lưu log vào trường logRunPostman
        // Lưu tạm Score để có thể dùng làm khóa ngoại cho Score_Detail
        scoreRepository.save(score);

        Map<Long, Float> parentScoreMap = new HashMap<>(); // Lưu điểm của chức năng cha
        float totalScoreAchieve = 0f; // Biến để lưu tổng điểm của tất cả các Score_Detail

        // Fetch all Postman_For_Grading for the given examPaperId, sorted by order
        // (e.g., sequence)
        List<Postman_For_Grading> postmanFunctions = postmanForGradingRepository
                .findByExamPaper_ExamPaperIdAndStatusTrueOrderByOrderPriorityAsc(examPaperId);

        for (Postman_For_Grading postmanFunction : postmanFunctions) {
            // Assuming there is no need to get the Exam_Question here directly,
            // otherwise you would join or fetch Exam_Questions based on each
            // Postman_For_Grading
            Exam_Question question = postmanFunction.getExamQuestion(); // Get the associated question

            // Create Score_Detail for each Postman_For_Grading
            Score_Detail scoreDetail = new Score_Detail();
            scoreDetail.setScore(score);
            scoreDetail.setExamQuestion(question);
            scoreDetail.setPostmanFunctionName(postmanFunction.getPostmanFunctionName());
            scoreDetail.setScoreOfFunction(postmanFunction.getScoreOfFunction());
            scoreDetail.setTotalPmtest(postmanFunction.getTotalPmTest());

            Long noPmtestAchieve = functionPassedCountMap.getOrDefault(postmanFunction.getPostmanFunctionName(), 0L);
            System.out.println("noPmtestAchieve for function " + postmanFunction.getPostmanFunctionName() + ": "
                    + noPmtestAchieve);
            reasonBuilder.append("noPmtestAchieve for function ")
                    .append(postmanFunction.getPostmanFunctionName())
                    .append(": ")
                    .append(noPmtestAchieve)
                    .append("\n");

            scoreDetail.setNoPmtestAchieve(noPmtestAchieve);

            // Tính scoreAchieve
            Float scoreAchieve = calculateScoreAchieve(postmanFunction, noPmtestAchieve, functionPassedCountMap,
                    parentScoreMap, reasonBuilder);
            scoreDetail.setScoreAchieve(scoreAchieve);

            // Cộng dồn scoreAchieve vào totalScoreAchieve
            totalScoreAchieve += scoreAchieve;

            // Nếu là chức năng cha, cập nhật parentScoreMap với scoreAchieve
            if (postmanFunction.getPostmanForGradingParentId() == null
                    || postmanFunction.getPostmanForGradingParentId()
                            .equals(postmanFunction.getPostmanForGradingId())) {
                parentScoreMap.put(postmanFunction.getPostmanForGradingId(), scoreAchieve);
                System.out.println("Updated parentScoreMap for function " + postmanFunction.getPostmanFunctionName()
                        + " with scoreAchieve: " + scoreAchieve);
                reasonBuilder.append("Updated parentScoreMap for function ")
                        .append(postmanFunction.getPostmanFunctionName())
                        .append(" with scoreAchieve: ")
                        .append(scoreAchieve)
                        .append("\n");
            }

            // Lưu scoreDetail vào database
            scoreDetailRepository.save(scoreDetail);
            System.out.println("Saved score detail for function " + postmanFunction.getPostmanFunctionName()
                    + ", scoreAchieve: " + scoreAchieve);
            reasonBuilder.append("Saved score detail for function ")
                    .append(postmanFunction.getPostmanFunctionName())
                    .append(", scoreAchieve: ")
                    .append(scoreAchieve)
                    .append("\n");
        }

        // Cập nhật lại tổng điểm vào Score
        score.setTotalScore(totalScoreAchieve);
        scoreRepository.save(score); // Lưu lại Score với totalScore đã cập nhật
        score.setReason(reasonBuilder.toString()); // Gán lý do vào trường reason
        System.out.println("Saved total score: " + totalScoreAchieve);
        reasonBuilder.append("Saved total score: ").append(totalScoreAchieve).append("\n");
        Optional<GradingProcess> optionalProcess = gradingProcessRepository
                .findByExamPaper_ExamPaperId(examPaper.getExamPaperId());
        if (!optionalProcess.isPresent()) {
            throw new NoSuchElementException("process not found");
        }
        GradingProcess gp = optionalProcess.get();
        gp.setSuccessProcess(gp.getSuccessProcess() + 1);
        sseController.pushEvent(gp.getProcessId(), "Grading", gp.getSuccessProcess(), gp.getTotalProcess(),
                gp.getStartDate());
        gradingProcessRepository.save(gp);
    }

    private Float calculateScoreAchieve(Postman_For_Grading postmanFunction, Long noPmtestAchieve,
            Map<String, Long> functionPassedCountMap, Map<Long, Float> parentScoreMap, StringBuilder reasonBuilder) {
        Long totalPmtest = postmanFunction.getTotalPmTest();
        Float scoreOfFunction = postmanFunction.getScoreOfFunction();
        Long parentId = postmanFunction.getPostmanForGradingParentId();

        // Xác định đây là một chức năng cha nếu parentId == null hoặc là tự tham chiếu
        // (trỏ tới chính nó)
        if (parentId == null || parentId.equals(postmanFunction.getPostmanForGradingId())) {
            Float scoreAchieve = (noPmtestAchieve / (float) totalPmtest) * scoreOfFunction;
            System.out.println("Calculating score for parent function: " + postmanFunction.getPostmanFunctionName()
                    + ", noPmtestAchieve: " + noPmtestAchieve
                    + ", totalPmtest: " + totalPmtest
                    + ", scoreAchieve: " + scoreAchieve);
            reasonBuilder.append("Calculating score for parent function: ")
                    .append(postmanFunction.getPostmanFunctionName())
                    .append(", noPmtestAchieve: ")
                    .append(noPmtestAchieve)
                    .append(", totalPmtest: ")
                    .append(totalPmtest)
                    .append(", scoreAchieve: ")
                    .append(scoreAchieve)
                    .append("\n");
            return scoreAchieve;
        } // Nếu là chức năng con và chức năng cha có scoreAchieve = 0
        else if (parentScoreMap.getOrDefault(parentId, 0.0f) == 0.0f) {
            Postman_For_Grading parentFunction = postmanForGradingRepository.findById(parentId).orElse(null);
            String parentFunctionName = (parentFunction != null) ? parentFunction.getPostmanFunctionName() : "Unknown";

            System.out.println("Parent function '" + parentFunctionName + "' has scoreAchieve = 0, so child function '"
                    + postmanFunction.getPostmanFunctionName() + "' will also have scoreAchieve = 0");
            reasonBuilder.append("Parent function '").append(parentFunctionName)
                    .append("' has scoreAchieve = 0, so child function '")
                    .append(postmanFunction.getPostmanFunctionName())
                    .append("' will also have scoreAchieve = 0\n");
            return 0.0f;
        }
        // Trường hợp khác
        Float scoreAchieve = (noPmtestAchieve / (float) totalPmtest) * scoreOfFunction;
        System.out.println("Calculating score for child function: " + postmanFunction.getPostmanFunctionName()
                + ", noPmtestAchieve: " + noPmtestAchieve
                + ", totalPmtest: " + totalPmtest
                + ", scoreAchieve: " + scoreAchieve);
        reasonBuilder.append("Calculating score for child function: ")
                .append(postmanFunction.getPostmanFunctionName())
                .append(", noPmtestAchieve: ")
                .append(noPmtestAchieve)
                .append(", totalPmtest: ")
                .append(totalPmtest)
                .append(", scoreAchieve: ")
                .append(scoreAchieve)
                .append("\n");
        return scoreAchieve;
    }

    private StudentDeployResult deployStudentSolution(StudentSourceInfoDTO studentSource) {
        Path dirPath = Paths.get(studentSource.getStudentSourceCodePath());
        Long studentId = studentSource.getStudentId(); // Lưu lại studentId để trả về kết quả

        // Executor service để quản lý các luồng
        ExecutorService executor = Executors.newFixedThreadPool(1); // Mỗi tiến trình chạy trong một luồng riêng biệt

        try {
            // Gửi công việc vào một luồng riêng biệt để thực thi lệnh docker-compose
            Future<StudentDeployResult> future = executor.submit(() -> {
                ProcessBuilder processBuilder = new ProcessBuilder("docker-compose", "up", "-d", "--build");
                processBuilder.directory(dirPath.toFile());

                // Cho phép tiến trình con sử dụng đầu ra của terminal
                processBuilder.inheritIO();

                try {
                    // Bắt đầu quá trình docker-compose
                    Process process = processBuilder.start();
                    int exitCode = process.waitFor();

                    if (exitCode == 0) {
                        return new StudentDeployResult(studentId, true, "Deploy thành công");
                    } else {
                        return new StudentDeployResult(studentId, false, "Deploy thất bại với mã thoát: " + exitCode);
                    }

                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                    return new StudentDeployResult(studentId, false, "Exception: " + e.getMessage());
                }
            });

            // Chờ cho tiến trình hoàn thành và nhận kết quả
            return future.get(); // Block cho đến khi tiến trình hoàn tất

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return new StudentDeployResult(studentId, false, "Exception: " + e.getMessage());
        } finally {
            // Đảm bảo tắt executor sau khi hoàn thành
            executor.shutdown();
        }
    }

    // private StudentDeployResult deployStudentSolution(StudentSourceInfoDTO
    // studentSource) {
    // Path dirPath = Paths.get(studentSource.getStudentSourceCodePath());
    // Long studentId = studentSource.getStudentId(); // Lưu lại studentId để trả về
    // kết quả
    // try {
    // ProcessBuilder processBuilder = new ProcessBuilder("docker-compose", "up",
    // "-d", "--build");
    // processBuilder.directory(dirPath.toFile());
    // processBuilder.inheritIO();
    // Process process = processBuilder.start();
    // int exitCode = process.waitFor();
    // if (exitCode == 0) {
    // return new StudentDeployResult(studentId, true, "Deploy thành công");
    // } else {
    // return new StudentDeployResult(studentId, false, "Deploy thất bại với mã
    // thoát: " + exitCode);
    // }
    // } catch (IOException | InterruptedException e) {
    // e.printStackTrace();
    // return new StudentDeployResult(studentId, false, "Exception: " +
    // e.getMessage());
    // }
    // }
    private void recordFailure(Long studentId, Long examPaperId, String reason) {
        Student student = scoreRepository.findStudentById(studentId);
        if (student != null && student.getOrganization() != null) {
            Score score = new Score();
            score.setStudent(student);

            Exam_Paper examPaper = new Exam_Paper();
            examPaper.setExamPaperId(examPaperId);
            score.setExamPaper(examPaper);
            score.setTotalScore(0.0f);
            score.setGradedAt(LocalDateTime.now());
            score.setReason(reason);
            scoreRepository.save(score);

        } else {
            System.err.println("Student or Organization not found for studentId: " + studentId);
        }
    }

    public void updateAppsettingsJson(Path filePath, Long examPaperId, int port) throws IOException {
        // Đọc nội dung file appsettings.json
        String content = Files.readString(filePath, StandardCharsets.UTF_8);
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode rootNode = (ObjectNode) objectMapper.readTree(content);

        // Thêm cấu hình "Kestrel" nếu chưa có
        if (!rootNode.has("Kestrel")) {
            ObjectNode kestrelNode = objectMapper.createObjectNode();
            ObjectNode endpointsNode = objectMapper.createObjectNode();
            ObjectNode httpNode = objectMapper.createObjectNode();
            httpNode.put("Url", "http://*:" + port);
            endpointsNode.set("Http", httpNode);
            kestrelNode.set("Endpoints", endpointsNode);
            rootNode.set("Kestrel", kestrelNode);
        } else {
            // Cập nhật cổng cho Kestrel nếu đã tồn tại
            ObjectNode kestrelNode = (ObjectNode) rootNode.get("Kestrel");
            ObjectNode endpointsNode = (ObjectNode) kestrelNode.get("Endpoints");
            ObjectNode httpNode = (ObjectNode) endpointsNode.get("Http");
            httpNode.put("Url", "http://*:" + port);
        }

        // Thay đổi "ConnectionStrings" dựa trên examPaperId
        String databaseName = examDatabaseRepository.findDatabaseNameByExamPaperId(examPaperId);
        if (rootNode.has("ConnectionStrings")) {
            ObjectNode connectionStringsNode = (ObjectNode) rootNode.get("ConnectionStrings");
            connectionStringsNode.fieldNames().forEachRemaining(key -> {
                connectionStringsNode.put(key, String.join(";",
                        "Server=" + PathUtil.DB_SERVER,
                        "uid=" + PathUtil.DB_UID,
                        "pwd=" + PathUtil.DB_PWD,
                        "database=" + databaseName,
                        "TrustServerCertificate=True"));
            });
        }

        // Ghi lại nội dung JSON vào file appsettings.json
        content = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
        Files.writeString(filePath, content, StandardCharsets.UTF_8);
    }

    public void findAndUpdateAppsettings(Path dirPath, Long examPaperId, int port) {
        try (Stream<Path> folders = Files.walk(dirPath, 1)) {
            List<Path> targetDirs = folders
                    .filter(Files::isDirectory)
                    .filter(path -> {
                        try (Stream<Path> files = Files.walk(path, 1)) {
                            return files.anyMatch(file -> file.getFileName().toString().equalsIgnoreCase("Program.cs"));
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());

            for (Path targetDir : targetDirs) {
                try (Stream<Path> paths = Files.walk(targetDir)) {
                    paths.filter(Files::isRegularFile)
                            .filter(path -> path.toString().endsWith("appsettings.json"))
                            .forEach(path -> {
                                try {
                                    updateAppsettingsJson(path, examPaperId, port);
                                } catch (IOException e) {
                                    // Log lỗi và tiếp tục thực thi
                                    System.err.println("Error updating: " + path + " - " + e.getMessage());
                                }
                            });
                } catch (IOException e) {
                    System.err
                            .println("Error walking through files in directory: " + targetDir + " - " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Error walking through directories: " + dirPath + " - " + e.getMessage());
        }
    }

    // public void findAndUpdateAppsettings(Path dirPath, Long examPaperId, int
    // port) throws IOException {
    // try (Stream<Path> folders = Files.walk(dirPath, 1)) {
    // List<Path> targetDirs = folders
    // .filter(Files::isDirectory)
    // .filter(path -> {
    // try (Stream<Path> files = Files.walk(path, 1)) {
    // return files.anyMatch(file ->
    // file.getFileName().toString().equalsIgnoreCase("Program.cs"));
    // } catch (IOException e) {
    // return false;
    // }
    // })
    // .collect(Collectors.toList());
    // for (Path targetDir : targetDirs) {
    // try (Stream<Path> paths = Files.walk(targetDir)) {
    // paths.filter(Files::isRegularFile)
    // .filter(path -> path.toString().endsWith("appsettings.json"))
    // .forEach(path -> {
    // try {
    // updateAppsettingsJson(path, examPaperId, port);
    // } catch (IOException e) {
    // System.err.println("Error updating: " + path + " - " + e.getMessage());
    // }
    // });
    // }
    // }
    // }
    // }
    private void deleteAndCreateDatabaseByExamPaperId(Long examPaperId) {
        try {
            Class.forName(PathUtil.DB_DRIVER);
            try (Connection connection = DriverManager.getConnection(PathUtil.DB_URL);
                    Statement statement = connection.createStatement()) {

                String databaseName = examDatabaseRepository.findDatabaseNameByExamPaperId(examPaperId);
                Exam_Database examDatabase = examDatabaseRepository.findByExamPaperExamPaperId(examPaperId);

                if (examDatabase != null) {
                    Long examDatabaseId = examDatabase.getExamDatabaseId();
                    System.out.println("Found Exam_Database ID: " + examDatabaseId);

                    if (databaseName != null && !databaseName.isEmpty()) {
                        String sql = "IF EXISTS (SELECT name FROM sys.databases WHERE name = '" + databaseName + "') "
                                + "BEGIN "
                                + "   ALTER DATABASE [" + databaseName + "] SET SINGLE_USER WITH ROLLBACK IMMEDIATE; "
                                + "   DROP DATABASE [" + databaseName + "]; "
                                + "END";
                        statement.executeUpdate(sql);
                        System.out.println("Database " + databaseName + " has been deleted.");
                    }

                    // Retrieve and use databaseScript instead of databaseFile
                    if (examDatabase.getDatabaseScript() != null) {
                        String createDatabaseSQL = examDatabase.getDatabaseScript();

                        // Split commands by "GO" keyword (case-insensitive)
                        String[] sqlCommands = createDatabaseSQL.split("(?i)\\bGO\\b");

                        for (String sqlCommand : sqlCommands) {
                            if (!sqlCommand.trim().isEmpty()) {
                                statement.executeUpdate(sqlCommand.trim());
                            }
                        }
                        System.out.println("Database " + databaseName + " has been created.");
                    } else {
                        System.out.println("No database script found for examPaperId: " + examPaperId);
                    }
                } else {
                    System.out.println("No Exam_Database found for examPaperId: " + examPaperId);
                }
            } catch (SQLException e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to delete and create database for examPaperId: " + examPaperId);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("SQL Server JDBC Driver not found.");
        }
    }

    public void deleteContainerAndImages() throws IOException {
        DockerClient dockerClient = DockerClientBuilder.getInstance("tcp://localhost:2375")
                .withDockerCmdExecFactory(new OkHttpDockerCmdExecFactory())
                .build();

        try {
            // Remove all containers
            List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();
            for (Container container : containers) {
                System.out.println("Removing container " + container.getNames()[0] + " ("
                        + container.getId().substring(0, 12) + ")");
                dockerClient.removeContainerCmd(container.getId()).withForce(true).exec();
            }
            System.out.println("All containers have been removed.");

            // Remove all images
            List<Image> images = dockerClient.listImagesCmd().withDanglingFilter(false).exec();
            for (Image image : images) {
                System.out.println("Removing image " + image.getId().substring(0, 12));
                dockerClient.removeImageCmd(image.getId()).withForce(true).exec();
            }
            System.out.println("All images have been removed.");

            // Remove unused networks
            List<Network> networks = dockerClient.listNetworksCmd().exec();
            for (Network network : networks) {
                // Ignore default Docker networks
                if (!network.getName().equals("bridge") && !network.getName().equals("host")
                        && !network.getName().equals("none")) {
                    System.out.println(
                            "Removing network " + network.getName() + " (" + network.getId().substring(0, 12) + ")");
                    dockerClient.removeNetworkCmd(network.getId()).exec();
                }
            }
            System.out.println("All unused networks have been removed.");

        } catch (DockerException e) {
            e.printStackTrace();
            throw new RuntimeException("Docker operation failed: " + e.getMessage());
        } finally {
            dockerClient.close(); // Close Docker client
        }
    }

    public void createFileCollectionPostman(Long examPaperId, Long sourceDetailId, int port) {
        // Fetch the exam paper's Postman file collection
        Exam_Paper examPaper = examPaperRepository.findById(examPaperId)
                .orElseThrow(() -> new RuntimeException("Exam_Paper not found with ID: " + examPaperId));

        byte[] fileCollection = examPaper.getFileCollectionPostman();
        if (fileCollection == null) {
            throw new RuntimeException("No fileCollectionPostman found in Exam_Paper with ID: " + examPaperId);
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode rootNode = (ObjectNode) objectMapper.readTree(fileCollection);
            ArrayNode items = (ArrayNode) rootNode.get("item");

            for (int i = 0; i < items.size(); i++) {
                ObjectNode item = (ObjectNode) items.get(i);
                if (item.has("request")) {
                    ObjectNode request = (ObjectNode) item.get("request");
                    if (request.has("url")) {
                        ObjectNode url = (ObjectNode) request.get("url");
                        String rawUrl = url.get("raw").asText();
                        String updatedRawUrl = rawUrl.replaceFirst("http://localhost:\\d+", "http://localhost:" + port);
                        url.put("raw", updatedRawUrl);
                        url.put("port", Integer.toString(port));
                    }
                }
            }

            // Convert updated JSON back to byte array and save it to sourceDetail
            byte[] updatedFileCollection = objectMapper.writeValueAsString(rootNode).getBytes(StandardCharsets.UTF_8);
            Source_Detail sourceDetail = sourceDetailRepository.findById(sourceDetailId)
                    .orElseThrow(() -> new RuntimeException("Source_Detail not found with ID: " + sourceDetailId));
            sourceDetail.setFileCollectionPostman(updatedFileCollection);
            sourceDetailRepository.save(sourceDetail);

            // Wait until fileCollectionPostman is confirmed as saved
            int maxRetries = 5;
            int retries = 0;
            while (retries < maxRetries) {
                sourceDetail = sourceDetailRepository.findById(sourceDetailId)
                        .orElseThrow(() -> new RuntimeException("Source_Detail not found with ID: " + sourceDetailId));

                if (sourceDetail.getFileCollectionPostman() != null) {
                    break; // Exit loop if the fileCollectionPostman is available
                }
                Thread.sleep(500); // Wait for a short time before retrying
                retries++;
            }

            if (sourceDetail.getFileCollectionPostman() == null) {
                throw new RuntimeException(
                        "fileCollectionPostman not properly saved for sourceDetailId: " + sourceDetailId);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to update fileCollectionPostman: " + e.getMessage(), e);
        }
    }

    // Phương thức khởi động Docker và kiểm tra trạng thái
    public boolean startDocker() {
        try {
            // Khởi động Docker Desktop
            Process process = new ProcessBuilder(
                    "cmd.exe", "/c", "start", "\"\"", "\"" + PathUtil.DOCKER_DESKTOP_PATH + "\"").start();

            // Đợi một vài giây cho Docker khởi động
            Thread.sleep(5000);

            // Kiểm tra trạng thái Docker bằng lệnh 'docker info'
            Process checkProcess = new ProcessBuilder("docker", "info").start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(checkProcess.getInputStream()));
            String line;
            StringBuilder output = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            checkProcess.waitFor();
            return checkProcess.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String runPostmanCollection(Long examPaperId) {
        Exam_Paper examPaper = examPaperRepository.findById(examPaperId).orElse(null);
        if (examPaper == null || examPaper.getFileCollectionPostman() == null) {
            System.err.println("Không tìm thấy Exam Paper hoặc fileCollectionPostman trống.");
            sseController.pushEvent(1l, "Exam Paper or fileCollectionPostman are null", 0, 10, LocalDateTime.now());
            return null;
        }

        try {
            // Tạo file tạm và ghi nội dung của fileCollectionPostman vào
            Path tempFile = Files.createTempFile("postman_collection_", ".json");
            Files.write(tempFile, examPaper.getFileCollectionPostman(), StandardOpenOption.WRITE);

            // Chạy newman với file tạm
            ProcessBuilder processBuilder = new ProcessBuilder(
                    PathUtil.NEWMAN_CMD_PATH,
                    "run",
                    tempFile.toAbsolutePath().toString());
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Đọc kết quả từ newman
            String result = new String(process.getInputStream().readAllBytes());
            String errorOutput = new String(process.getErrorStream().readAllBytes()); // Đọc lỗi nếu có

            // Kiểm tra nếu có lỗi
            if (!errorOutput.isEmpty()) {
                System.err.println("Error running Newman: " + errorOutput);
                return null;
            }

            // Xóa file tạm sau khi sử dụng
            Files.deleteIfExists(tempFile);

            return result;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // private boolean comparePostmanResults(String postmanResult, Long examPaperId) {
    //     // Tạo mảng từ kết quả Postman bằng cách lấy chuỗi sau dấu "→" trong mỗi dòng
    //     List<String> postmanOutputFunctions = Arrays.stream(postmanResult.split("\n"))
    //             .filter(line -> line.contains("→"))
    //             .map(line -> line.substring(line.indexOf("→") + 1).trim())
    //             .collect(Collectors.toList());

    //     // Lấy danh sách postmanFunctionName từ Postman_For_Grading theo orderBy
    //     List<String> postmanFunctionNames = postmanForGradingRepository
    //             .findByExamQuestion_ExamPaper_ExamPaperId(examPaperId)
    //             .stream()
    //             .sorted(Comparator.comparing(Postman_For_Grading::getOrderBy))
    //             .map(Postman_For_Grading::getPostmanFunctionName)
    //             .collect(Collectors.toList());

    //     // So sánh hai mảng
    //     return postmanOutputFunctions.equals(postmanFunctionNames);
    // }

    // private boolean comparePostmanResults(String postmanResult, Long examPaperId) {
    //     // Tạo mảng từ kết quả Postman bằng cách lấy chuỗi sau dấu "→" trong mỗi dòng
    //     List<String> postmanOutputFunctions = Arrays.stream(postmanResult.split("\n"))
    //             .filter(line -> line.contains("→"))
    //             .map(line -> line.substring(line.indexOf("→") + 1).trim())
    //             .collect(Collectors.toList());

    //     // Lấy danh sách postmanFunctionName từ Postman_For_Grading theo order Priority
    //     List<String> postmanFunctionNames = postmanForGradingRepository
    //             .findByExamQuestion_ExamPaper_ExamPaperId(examPaperId)
    //             .stream()
              
    //             .sorted(Comparator.comparing(Postman_For_Grading::getOrderPriority))
    //             .map(Postman_For_Grading::getPostmanFunctionName)
    //             .collect(Collectors.toList());

    //     // So sánh hai mảng
    //     return postmanOutputFunctions.equals(postmanFunctionNames);
    // }

    // private boolean comparePostmanResults(String postmanResult, Long examPaperId) {
    //     // Tạo mảng từ kết quả Postman bằng cách lấy chuỗi sau dấu "→" và bỏ qua dấu cách
    //     List<String> postmanOutputFunctions = Arrays.stream(postmanResult.split("\n"))
    //             .filter(line -> line.contains("→"))
    //             .map(line -> {
    //                 int arrowIndex = line.indexOf("→");
    //                 if (arrowIndex != -1 && arrowIndex + 1 < line.length()) {
    //                     return line.substring(arrowIndex + 2).trim(); // Bỏ qua dấu cách sau dấu "→"
    //                 }
    //                 return ""; // Nếu không có dấu "→", trả về chuỗi rỗng
    //             })
    //             .collect(Collectors.toList());
    
    //     // Lấy danh sách Postman_For_Grading từ repository
    //     List<Postman_For_Grading> postmanGradings = postmanForGradingRepository
    //             .findByExamQuestion_ExamPaper_ExamPaperId(examPaperId)
    //             .stream()
    //             .sorted(Comparator.comparing(Postman_For_Grading::getOrderPriority))
    //             .collect(Collectors.toList());
    
    //     // Kiểm tra tính hợp lệ của orderPriority (bắt đầu từ 1 và tăng dần)
    //     for (int i = 0; i < postmanGradings.size(); i++) {
    //         if (!postmanGradings.get(i).getOrderPriority().equals((long) (i + 1))) {
    //             System.err.println("Lỗi: orderPriority không hợp lệ (không bắt đầu từ 1 hoặc không tăng dần).");
    //             return false;
    //         }
    //     }
    
    //     // Lấy danh sách postmanFunctionName từ danh sách đã kiểm tra
    //     List<String> postmanFunctionNames = postmanGradings.stream()
    //             .map(Postman_For_Grading::getPostmanFunctionName)
    //             .collect(Collectors.toList());
    
    //     // So sánh hai mảng
    //     return postmanOutputFunctions.equals(postmanFunctionNames);
    // }
    
    

    // private boolean comparePostmanResults(String postmanResult, Long examPaperId) {
    //     // Tạo mảng từ kết quả Postman bằng cách lấy chuỗi sau dấu "→" và bỏ qua dấu cách
    //     List<String> postmanOutputFunctions = Arrays.stream(postmanResult.split("\n"))
    //             .filter(line -> line.contains("→"))
    //             .map(line -> {
    //                 int arrowIndex = line.indexOf("→");
    //                 if (arrowIndex != -1 && arrowIndex + 1 < line.length()) {
    //                     return line.substring(arrowIndex + 2).trim(); // Bỏ qua dấu cách sau dấu "→"
    //                 }
    //                 return ""; // Nếu không có dấu "→", trả về chuỗi rỗng
    //             })
    //             .collect(Collectors.toList());
    
    //     // Lấy danh sách Postman_For_Grading từ repository kèm theo điều kiện status = true
    //     List<Postman_For_Grading> postmanGradings = postmanForGradingRepository
    //     .findByExamPaper_ExamPaperIdAndStatus(examPaperId, true)
    //     .stream()
    //     .sorted(Comparator.comparing(Postman_For_Grading::getOrderPriority))
    //     .collect(Collectors.toList());
    
    //     // Kiểm tra tính hợp lệ của orderPriority (bắt đầu từ 1 và tăng dần)
    //     for (int i = 0; i < postmanGradings.size(); i++) {
    //         if (!postmanGradings.get(i).getOrderPriority().equals((long) (i + 1))) {
    //             System.err.println("Lỗi: orderPriority không hợp lệ (không bắt đầu từ 1 hoặc không tăng dần).");
    //             return false;
    //         }
    //     }
    
    //     // Lấy danh sách postmanFunctionName từ danh sách đã kiểm tra
    //     List<String> postmanFunctionNames = postmanGradings.stream()
    //             .map(Postman_For_Grading::getPostmanFunctionName)
    //             .collect(Collectors.toList());
    
    //     // So sánh hai mảng
    //     return postmanOutputFunctions.equals(postmanFunctionNames);
    // }
    private boolean comparePostmanResults(String postmanResult, Long examPaperId) {
        // Tạo mảng từ kết quả Postman bằng cách lấy chuỗi sau dấu "→" và bỏ qua dấu cách
        List<String> postmanOutputFunctions = Arrays.stream(postmanResult.split("\n"))
                .filter(line -> line.contains("→"))
                .map(line -> {
                    int arrowIndex = line.indexOf("→");
                    if (arrowIndex != -1 && arrowIndex + 1 < line.length()) {
                        return line.substring(arrowIndex + 2).trim(); // Bỏ qua dấu cách sau dấu "→"
                    }
                    return ""; // Nếu không có dấu "→", trả về chuỗi rỗng
                })
                .collect(Collectors.toList());
    
        // Lấy danh sách Postman_For_Grading từ repository kèm theo điều kiện status = true và sắp xếp theo orderPriority
        List<Postman_For_Grading> postmanGradings = postmanForGradingRepository
                .findByExamPaper_ExamPaperIdAndStatusTrueOrderByOrderPriorityAsc(examPaperId);
    
        // Kiểm tra tính hợp lệ của orderPriority (bắt đầu từ 1 và tăng dần)
        for (int i = 0; i < postmanGradings.size(); i++) {
            if (!postmanGradings.get(i).getOrderPriority().equals((long) (i + 1))) {
                System.err.println("Lỗi: orderPriority không hợp lệ (không bắt đầu từ 1 hoặc không tăng dần).");
                return false;
            }
        }
    
        // Lấy danh sách postmanFunctionName từ danh sách đã kiểm tra
        List<String> postmanFunctionNames = postmanGradings.stream()
                .map(Postman_For_Grading::getPostmanFunctionName)
                .collect(Collectors.toList());
    
        // So sánh hai mảng
        return postmanOutputFunctions.equals(postmanFunctionNames);
    }
    

    
    public static Map.Entry<Path, String> findCsprojAndDotnetVersion(Path dirPath) throws IOException {
        Pattern pattern = Pattern.compile("<TargetFramework>(net\\d+\\.\\d+)</TargetFramework>");

        try (Stream<Path> folders = Files.walk(dirPath, 1)) {
            // Find directories that contain a Program.cs file
            Optional<Path> targetDir = folders
                    .filter(Files::isDirectory)
                    .filter(path -> {
                        try (Stream<Path> files = Files.walk(path, 1)) {
                            return files.anyMatch(file -> file.getFileName().toString().equalsIgnoreCase("Program.cs"));
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .findFirst();

            if (targetDir.isPresent()) {
                try (Stream<Path> paths = Files.walk(targetDir.get())) {
                    for (Path path : paths.filter(Files::isRegularFile).collect(Collectors.toList())) {
                        if (path.toString().endsWith(".csproj")) {
                            List<String> lines = Files.readAllLines(path);
                            for (String line : lines) {
                                Matcher matcher = pattern.matcher(line);
                                if (matcher.find()) {
                                    String dotnetVersion = matcher.group(1).replace("net", "");
                                    return new AbstractMap.SimpleEntry<>(path, dotnetVersion);
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public static void createDockerfile(Path dirPath, Path csprojPath, String dotnetVersion, int port)
            throws IOException {
        String csprojName = csprojPath.getFileName().toString();
        String folderName = csprojPath.getParent().getFileName().toString();

        String dockerfileContent = String.format("""
                FROM mcr.microsoft.com/dotnet/aspnet:%s AS base
                WORKDIR /app
                EXPOSE %d

                FROM mcr.microsoft.com/dotnet/sdk:%s AS build
                WORKDIR /src
                COPY ["%s/%s", "./"]
                RUN dotnet restore "./%s"
                COPY . .
                WORKDIR "/src/."
                RUN dotnet build "%s/%s" -c Release -o /app/build

                FROM build AS publish
                RUN dotnet publish "%s/%s" -c Release -o /app/publish

                FROM base AS final
                WORKDIR /app
                COPY --from=publish /app/publish .
                ENTRYPOINT ["dotnet", "%s"]
                """, dotnetVersion, port, dotnetVersion, folderName, csprojName, csprojName, folderName, csprojName,
                folderName, csprojName, csprojName.replace(".csproj", ".dll"));

        try (BufferedWriter writer = Files.newBufferedWriter(dirPath.resolve("Dockerfile"))) {
            writer.write(dockerfileContent);
        }
    }

    public static void createDockerCompose(Path dirPath, Long studentId, int port) throws IOException {
        String dockerComposeContent = String.format("""
                services:
                  project-studentid-%d-%d:
                    image: project-studentid-%d-%d
                    build:
                      context: .
                      dockerfile: Dockerfile
                    ports:
                      - "%d:%d"
                """, studentId, port, studentId, port, port, port);

        try (BufferedWriter writer = Files.newBufferedWriter(dirPath.resolve("docker-compose.yml"))) {
            writer.write(dockerComposeContent);
        }
    }

    public static void removeDockerFiles(Path dirPath) throws IOException {
        Files.deleteIfExists(dirPath.resolve("Dockerfile"));
        Files.deleteIfExists(dirPath.resolve("docker-compose.yml"));
    }

    public static void deleteAllFilesAndFolders(String directoryPath) {
        Path directory = Paths.get(directoryPath);

        try {
            // Duyệt qua tất cả các tệp và thư mục con
            Files.walkFileTree(directory, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    // Xóa từng tệp
                    Files.delete(file);
                    System.out.println("Deleted file: " + file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    // Xóa thư mục sau khi đã xóa hết các tệp bên trong
                    Files.delete(dir);
                    System.out.println("Deleted directory: " + dir);
                    return FileVisitResult.CONTINUE;
                }
            });

            System.out.println("All files and folders deleted successfully.");
        } catch (IOException e) {
            System.err.println("Error while deleting files and folders: " + e.getMessage());
        }
    }

}
