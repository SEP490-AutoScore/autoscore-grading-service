package com.CodeEvalCrew.AutoScore.services.autoscore_postman_service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.CodeEvalCrew.AutoScore.controllers.SSEController;
import com.CodeEvalCrew.AutoScore.models.DTO.ResponseDTO.StudentDeployResult;
import com.CodeEvalCrew.AutoScore.models.DTO.StudentSourceInfoDTO;
import com.CodeEvalCrew.AutoScore.models.Entity.Enum.GradingStatusEnum;
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
import com.CodeEvalCrew.AutoScore.repositories.examdatabase_repository.IExamDatabaseRepository;
import com.CodeEvalCrew.AutoScore.repositories.grading_process_repository.GradingProcessRepository;
import com.CodeEvalCrew.AutoScore.repositories.postman_for_grading.PostmanForGradingRepository;
import com.CodeEvalCrew.AutoScore.repositories.score_detail_repository.ScoreDetailRepository;
import com.CodeEvalCrew.AutoScore.repositories.score_repository.ScoreRepository;
import com.CodeEvalCrew.AutoScore.repositories.source_repository.SourceDetailRepository;
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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

@Service
public class AutoscorePostmanService implements IAutoscorePostmanService {

    @Value("${number.deploy}")
    private int numberDeploy;

    @Value("${base.port}")
    private int basePort;

    @Value("${db.uid}")
    private String dbUid;

    @Value("${db.pwd}")
    private String dbPwd;

    @Value("${db.driver}")
    private String dbDriver;

    @Value("${db.url}")
    private String dbUrl;

    @Value("${docker.host}")
    private String dockerHost;

    @Autowired
    private GradingProcessRepository gradingProcessRepository;
    @Autowired
    private SourceDetailRepository sourceDetailRepository;
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
    private PostmanForGradingRepository postmanForGradingRepository;
    @Autowired
    private SSEController sseController;
    @Autowired
    private PathUtil PathUtil;

    @Override
    public List<StudentSourceInfoDTO> gradingFunction(List<StudentSourceInfoDTO> studentSources,
            Long examPaperId) {

        PathUtil.getConfigMemoryProcessor();
        // int numberDeploy = PathUtil.NUMBER_DEPLOY;

        Optional<Exam_Paper> optionalExamPaper = examPaperRepository.findById(examPaperId);
        if (optionalExamPaper.isEmpty()) {
            System.err.println("Exam Paper not exits");

            sseController.pushGradingProcess(0l, GradingStatusEnum.ERROR, LocalDateTime.now(), examPaperId);

            return null;
        }

        Exam_Paper examPaper = optionalExamPaper.get();
        if (examPaper.getFileCollectionPostman() == null || examPaper.getFileCollectionPostman().length == 0) {
            System.err.println("No data of Postman Collection.");

            sseController.pushGradingProcess(0l, GradingStatusEnum.ERROR, LocalDateTime.now(), examPaperId);

            return null;
        }

        if (!Boolean.TRUE.equals(examPaper.getIsComfirmFile())) {
            System.err.println("File postman not confirm");

            sseController.pushGradingProcess(0l, GradingStatusEnum.ERROR, LocalDateTime.now(), examPaperId);

            return null;
        }

        Optional<Exam_Database> optionalExamDatabase = examDatabaseRepository.findById(examPaper.getExamPaperId());
        if (optionalExamDatabase.isEmpty()) {
            System.err.println("Exam Database not exits");

            sseController.pushGradingProcess(0l, GradingStatusEnum.ERROR, LocalDateTime.now(), examPaperId);

            return null;
        }

        String postmanResult = runPostmanCollection(examPaperId);
        if (postmanResult == null) {
            System.err.println("Can not run file Postman Collection.");

            sseController.pushGradingProcess(0l, GradingStatusEnum.ERROR, LocalDateTime.now(), examPaperId);

            return null;
        }

        if (!comparePostmanResults(postmanResult, examPaperId)) {
            System.err.println("Please import file postman again.");
            return null;
        }

        if (!startDocker()) {
            System.err.println("Check docker");
            return null;
        }

        deleteAndCreateDatabaseByExamPaperId(examPaperId);

        processStudentSolutions(studentSources, examPaperId, numberDeploy);

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

            List<StudentSourceInfoDTO> successfulDeployments = new ArrayList<>();

            for (int i = 0; i < currentBatch.size(); i++) {
                StudentSourceInfoDTO studentSource = currentBatch.get(i);
                Path dirPath = Paths.get(studentSource.getStudentSourceCodePath());
                int port = basePort + i;
                Long studentId = studentSource.getStudentId();

                createFileCollectionPostman(examPaperId, studentSource.getSourceDetailId(), port);

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

            if (!successfulDeployments.isEmpty()) {
                for (StudentSourceInfoDTO successfulStudent : successfulDeployments) {

                    Pair<Map<String, Integer>, String> resultAndLog = getAndRunPostmanCollection(
                            successfulStudent.getStudentId(), successfulStudent.getSourceDetailId());

                    Map<String, Integer> passedFunctionNames = resultAndLog.getLeft();
                    String logBuilder = resultAndLog.getRight();

                    Map<String, Long> functionPassedCountMap = passedFunctionNames.entrySet().stream()
                            .collect(Collectors.groupingBy(Map.Entry::getKey,
                                    Collectors.summingLong(Map.Entry::getValue)));

                    System.out.println(
                            "Function passed count map for studentId " + successfulStudent.getStudentId() + ": "
                                    + functionPassedCountMap);

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
        StringBuilder logBuilder = new StringBuilder();

        try {

            Source_Detail sourceDetail = sourceDetailRepository.findById(sourceDetailId)
                    .orElseThrow(() -> new RuntimeException("Source_Detail not found with ID: " + sourceDetailId));

            byte[] postmanCollection = sourceDetail.getFileCollectionPostman();
            Objects.requireNonNull(postmanCollection,
                    "fileCollectionPostman is null for sourceDetailId: " + sourceDetailId);

            Path tempPostmanFile = Files.createTempFile(studentId.toString(), ".json");
            Files.write(tempPostmanFile, postmanCollection);

            System.out.println("Temporary Postman Collection created at: " + tempPostmanFile);
            System.out.println("Running Newman for studentId: " + studentId);

            String newmanCmdPath = PathUtil.getNewmanCmdPath();
            ProcessBuilder processBuilder = new ProcessBuilder(newmanCmdPath, "run", tempPostmanFile.toString());
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    logBuilder.append(line).append("\n");

                    if (line.startsWith("→")) {
                        if (currentFunction != null) {
                            functionResults.put(currentFunction, passCount);
                        }
                        currentFunction = line.substring(2).trim();
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
                System.err.println("Newman execution failed with exit code: " + exitCode);
            } else {
                System.out.println("Newman executed successfully.");
            }

            Files.deleteIfExists(tempPostmanFile);
            System.out.println("Temporary Postman Collection deleted.");

        } catch (Exception e) {
            e.printStackTrace();
        }

        return Pair.of(functionResults, logBuilder.toString());
    }

    // private Pair<Map<String, Integer>, String> getAndRunPostmanCollection(Long
    // studentId, Long sourceDetailId) {
    // Map<String, Integer> functionResults = new HashMap<>();
    // String currentFunction = null;
    // int passCount = 0;
    // StringBuilder logBuilder = new StringBuilder();

    // try {

    // Path studentDir = Paths.get(PathUtil.DIRECTORY_PATH,
    // String.valueOf(studentId));
    // Files.createDirectories(studentDir);

    // Source_Detail sourceDetail = sourceDetailRepository.findById(sourceDetailId)
    // .orElseThrow(() -> new RuntimeException("Source_Detail not found with ID: " +
    // sourceDetailId));

    // Path postmanFilePath = studentDir.resolve(studentId + ".json");

    // byte[] postmanCollection = sourceDetail.getFileCollectionPostman();
    // Objects.requireNonNull(postmanCollection,
    // "fileCollectionPostman is null for sourceDetailId: " + sourceDetailId);
    // Files.write(postmanFilePath, postmanCollection);

    // // wait file to create success
    // int waitTimeInSeconds = 10;
    // int intervalInMilliseconds = 500;
    // int waited = 0;
    // while (!Files.exists(postmanFilePath) && waited < waitTimeInSeconds * 1000) {
    // Thread.sleep(intervalInMilliseconds);
    // waited += intervalInMilliseconds;
    // }

    // if (!Files.exists(postmanFilePath)) {
    // throw new IOException("Failed to create Postman collection file within
    // timeout.");
    // }

    // System.out.println("Running Newman for studentId: " + studentId);

    // String newmanCmdPath = PathUtil.getNewmanCmdPath();

    // ProcessBuilder processBuilder = new ProcessBuilder(newmanCmdPath, "run",
    // postmanFilePath.toString());

    // // ProcessBuilder processBuilder = new
    // ProcessBuilder(PathUtil.NEWMAN_CMD_PATH, "run",
    // // postmanFilePath.toString());

    // processBuilder.redirectErrorStream(true);
    // Process process = processBuilder.start();

    // // name file to save output
    // Path outputFile = studentDir.resolve(studentId + ".txt");

    // try (
    // BufferedReader reader = new BufferedReader(
    // new InputStreamReader(process.getInputStream(), "UTF-8"));
    // BufferedWriter writer = Files.newBufferedWriter(outputFile)) {

    // String line;
    // while ((line = reader.readLine()) != null) {
    // writer.write(line);
    // writer.newLine();

    // line = line.trim();
    // logBuilder.append(line).append("\n");

    // if (line.startsWith("→")) {
    // if (currentFunction != null) {
    // functionResults.put(currentFunction, passCount);
    // }

    // currentFunction = line.substring(2).trim(); // Get the function name after
    // "→"
    // passCount = 0;
    // } else if (line.startsWith("√")) {
    // passCount++;
    // }
    // }

    // if (currentFunction != null) {
    // functionResults.put(currentFunction, passCount);
    // }

    // }

    // int exitCode = process.waitFor();
    // if (exitCode != 0) {
    // System.out.println("Newman execution failed with exit code: " + exitCode);
    // } else {
    // System.out.println("Newman executed successfully.");
    // }

    // } catch (Exception e) {
    // e.printStackTrace();
    // }

    // // Return both function results and the log as a Pair
    // return Pair.of(functionResults, logBuilder.toString());
    // }

    public void saveScoreAndScoreDetail(Long studentId, Long examPaperId,
            Map<String, Long> functionPassedCountMap, String logBuilder) {
        Student student = studentRepository.findById(studentId).orElse(null);
        Exam_Paper examPaper = examPaperRepository.findById(examPaperId).orElse(null);

        if (student == null || examPaper == null) {
            System.err.println("Student or Exam Paper not exits");
            return;
        }

        StringBuilder reasonBuilder = new StringBuilder();

        Score score = scoreRepository.findByStudentIdAndExamPaperId(studentId, examPaperId);

        if (score == null) {
            score = new Score();
            score.setStudent(student);
            score.setExamPaper(examPaper);
            score.setGradedAt(LocalDateTime.now());

        } else {
            score.getScoreDetails().clear();
            scoreRepository.save(score);
        }
        score.setLogRunPostman(null);
        score.setLogRunPostman(logBuilder.toString());
        scoreRepository.save(score);

        Map<Long, Float> parentScoreMap = new HashMap<>();
        float totalScoreAchieve = 0f;

        List<Postman_For_Grading> postmanFunctions = postmanForGradingRepository
                .findByExamPaper_ExamPaperIdAndStatusTrueOrderByOrderPriorityAsc(examPaperId);

        for (Postman_For_Grading postmanFunction : postmanFunctions) {

            Exam_Question question = postmanFunction.getExamQuestion();

            Score_Detail scoreDetail = new Score_Detail();
            scoreDetail.setScore(score);

            if (question != null) {
                scoreDetail.setExamQuestion(question);
            }
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

            Float scoreAchieve = calculateScoreAchieve(postmanFunction, noPmtestAchieve, functionPassedCountMap,
                    parentScoreMap, reasonBuilder);
            scoreDetail.setScoreAchieve(scoreAchieve);

            totalScoreAchieve += scoreAchieve;

            if (postmanFunction.getPostmanForGradingParentId() == null
                    || postmanFunction.getPostmanForGradingParentId()
                            .equals(postmanFunction.getPostmanForGradingId())) {
                parentScoreMap.put(postmanFunction.getPostmanForGradingId(), scoreAchieve);
                System.out.println("Updated parent function: " + postmanFunction.getPostmanFunctionName()
                        + " with scoreAchieve: " + scoreAchieve);
                reasonBuilder.append("Updated parent function: ")
                        .append(postmanFunction.getPostmanFunctionName())
                        .append(" with scoreAchieve: ")
                        .append(scoreAchieve)
                        .append("\n");
            }

            scoreDetailRepository.save(scoreDetail);

            System.out.println("Saved score detail for function " + postmanFunction.getPostmanFunctionName()
                    + ", scoreAchieve: " + scoreAchieve);
            reasonBuilder.append("Saved score detail for function ")
                    .append(postmanFunction.getPostmanFunctionName())
                    .append(", scoreAchieve: ")
                    .append(scoreAchieve)
                    .append("\n");
        }

        score.setTotalScore(totalScoreAchieve);
        score.setReason(reasonBuilder.toString());
        scoreRepository.save(score);

        System.out.println("Saved total score: " + totalScoreAchieve);
        reasonBuilder.append("Saved total score: ").append(totalScoreAchieve).append("\n");
        Optional<GradingProcess> optionalProcess = gradingProcessRepository
                .findByExamPaper_ExamPaperId(examPaper.getExamPaperId());
        if (!optionalProcess.isPresent()) {
            throw new NoSuchElementException("process not found");
        }
        GradingProcess gp = optionalProcess.get();
        sseController.pushGradingProcess(gp.getProcessId(), gp.getStatus(), gp.getStartDate(), examPaperId);
        gradingProcessRepository.save(gp);
    }

    private Float calculateScoreAchieve(Postman_For_Grading postmanFunction, Long noPmtestAchieve,
            Map<String, Long> functionPassedCountMap, Map<Long, Float> parentScoreMap, StringBuilder reasonBuilder) {
        Long totalPmtest = postmanFunction.getTotalPmTest();
        Float scoreOfFunction = postmanFunction.getScoreOfFunction();
        Long parentId = postmanFunction.getPostmanForGradingParentId();

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
        } else if (parentScoreMap.getOrDefault(parentId, 0.0f) == 0.0f) {
            Postman_For_Grading parentFunction = postmanForGradingRepository.findById(parentId).orElse(null);
            String parentFunctionName = (parentFunction != null) ? parentFunction.getPostmanFunctionName() : "Unknown";

            System.out.println("Parent function: " + parentFunctionName + "has scoreAchieve = 0, so child function: "
                    + postmanFunction.getPostmanFunctionName() + "will also have scoreAchieve = 0");
            reasonBuilder.append("Parent function: ").append(parentFunctionName)
                    .append("has scoreAchieve = 0, so child function: ")
                    .append(postmanFunction.getPostmanFunctionName())
                    .append("will also have scoreAchieve = 0\n");
            return 0.0f;
        }

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
        Long studentId = studentSource.getStudentId();

        ExecutorService executor = Executors.newFixedThreadPool(1);

        try {

            Future<StudentDeployResult> future = executor.submit(() -> {
                ProcessBuilder processBuilder = new ProcessBuilder("docker-compose", "up", "-d", "--build");
                processBuilder.directory(dirPath.toFile());

                processBuilder.inheritIO();

                try {

                    Process process = processBuilder.start();
                    int exitCode = process.waitFor();

                    if (exitCode == 0) {
                        return new StudentDeployResult(studentId, true, "Deploy success");
                    } else {
                        return new StudentDeployResult(studentId, false, "Deploy fail with exitcode: " + exitCode);
                    }

                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                    return new StudentDeployResult(studentId, false, "Exception: " + e.getMessage());
                }
            });

            return future.get();

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return new StudentDeployResult(studentId, false, "Exception: " + e.getMessage());
        } finally {

            executor.shutdown();
        }
    }

    private void recordFailure(Long studentId, Long examPaperId, String reason) {
        Student student = scoreRepository.findStudentById(studentId);

        if (student != null) {
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
            System.err.println("Student not found for studentId: " + studentId);
        }
    }

    public void updateAppsettingsJson(Path filePath, Long examPaperId, int port) throws IOException {

        JsonObject rootObject;
        try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
                JsonReader jsonReader = new JsonReader(reader)) {
            jsonReader.setLenient(true);
            rootObject = JsonParser.parseReader(jsonReader).getAsJsonObject();
        }

        if (!rootObject.has("Kestrel")) {
            JsonObject kestrelObject = new JsonObject();
            JsonObject endpointsObject = new JsonObject();
            JsonObject httpObject = new JsonObject();
            httpObject.addProperty("Url", "http://*:" + port);
            endpointsObject.add("Http", httpObject);
            kestrelObject.add("Endpoints", endpointsObject);
            rootObject.add("Kestrel", kestrelObject);
        } else {
            JsonObject kestrelObject = rootObject.getAsJsonObject("Kestrel");
            JsonObject endpointsObject = kestrelObject.getAsJsonObject("Endpoints");
            JsonObject httpObject = endpointsObject.getAsJsonObject("Http");
            if (httpObject != null) {
                httpObject.addProperty("Url", "http://*:" + port);
            }
        }

        String databaseName = examDatabaseRepository.findDatabaseNameByExamPaperId(examPaperId);
        if (rootObject.has("ConnectionStrings")) {
            JsonObject connectionStringsObject = rootObject.getAsJsonObject("ConnectionStrings");
            String dbServer = PathUtil.getDbServer();
            for (Map.Entry<String, JsonElement> entry : connectionStringsObject.entrySet()) {
                connectionStringsObject.addProperty(entry.getKey(), String.join(";",
                        "Server=" + dbServer,
                        "uid=" + dbUid,
                        "pwd=" + dbPwd,
                        "database=" + databaseName,
                        "TrustServerCertificate=True"));
            }
        }

        // try (Writer writer = Files.newBufferedWriter(filePath,
        // StandardCharsets.UTF_8)) {
        // Gson gson = new GsonBuilder().setPrettyPrinting().create();
        // gson.toJson(rootObject, writer);
        // }
        try (Writer writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .disableHtmlEscaping() // Tắt mã hóa ký tự đặc biệt
                    .create();
            gson.toJson(rootObject, writer);
        }

    }

    // public void updateAppsettingsJson(Path filePath, Long examPaperId, int port)
    // throws IOException {

    // String content = Files.readString(filePath, StandardCharsets.UTF_8);
    // ObjectMapper objectMapper = new ObjectMapper();
    // ObjectNode rootNode = (ObjectNode) objectMapper.readTree(content);

    // if (!rootNode.has("Kestrel")) {
    // ObjectNode kestrelNode = objectMapper.createObjectNode();
    // ObjectNode endpointsNode = objectMapper.createObjectNode();
    // ObjectNode httpNode = objectMapper.createObjectNode();
    // httpNode.put("Url", "http://*:" + port);
    // endpointsNode.set("Http", httpNode);
    // kestrelNode.set("Endpoints", endpointsNode);
    // rootNode.set("Kestrel", kestrelNode);
    // } else {

    // ObjectNode kestrelNode = (ObjectNode) rootNode.get("Kestrel");
    // ObjectNode endpointsNode = (ObjectNode) kestrelNode.get("Endpoints");
    // ObjectNode httpNode = (ObjectNode) endpointsNode.get("Http");
    // httpNode.put("Url", "http://*:" + port);
    // }

    // String databaseName =
    // examDatabaseRepository.findDatabaseNameByExamPaperId(examPaperId);
    // if (rootNode.has("ConnectionStrings")) {
    // ObjectNode connectionStringsNode = (ObjectNode)
    // rootNode.get("ConnectionStrings");
    // String dbServer = PathUtil.getDbServer();
    // connectionStringsNode.fieldNames().forEachRemaining(key -> {
    // connectionStringsNode.put(key, String.join(";",
    // "Server=" + dbServer,
    // "uid=" + PathUtil.DB_UID,
    // "pwd=" + PathUtil.DB_PWD,
    // "database=" + databaseName,
    // "TrustServerCertificate=True"));
    // });
    // }

    // content =
    // objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
    // Files.writeString(filePath, content, StandardCharsets.UTF_8);
    // }

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

    private void deleteAndCreateDatabaseByExamPaperId(Long examPaperId) {
        try {
            Class.forName(dbDriver);
            try (Connection connection = DriverManager.getConnection(dbUrl);
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
        DockerClient dockerClient = DockerClientBuilder.getInstance(dockerHost)
                .withDockerCmdExecFactory(new OkHttpDockerCmdExecFactory())
                .build();

        try {
            List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();
            for (Container container : containers) {
                System.out.println("Removing container " + container.getNames()[0] + " ("
                        + container.getId().substring(0, 12) + ")");
                dockerClient.removeContainerCmd(container.getId()).withForce(true).exec();
            }
            System.out.println("All containers have been removed.");

            List<Image> images = dockerClient.listImagesCmd().withDanglingFilter(false).exec();
            for (Image image : images) {
                System.out.println("Removing image " + image.getId().substring(0, 12));
                dockerClient.removeImageCmd(image.getId()).withForce(true).exec();
            }
            System.out.println("All images have been removed.");

            List<Network> networks = dockerClient.listNetworksCmd().exec();
            for (Network network : networks) {
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
            dockerClient.close();
        }
    }

    public void createFileCollectionPostman(Long examPaperId, Long sourceDetailId, int port) {
        Exam_Paper examPaper = examPaperRepository.findById(examPaperId)
                .orElseThrow(() -> new RuntimeException("Exam_Paper not found with ID: " + examPaperId));

        byte[] fileCollection = examPaper.getFileCollectionPostman();
        if (fileCollection == null) {
            throw new RuntimeException("No fileCollectionPostman found in Exam_Paper with ID: " + examPaperId);
        }

        try {
            // Convert byte[] to String with UTF-8
            String fileCollectionString = new String(fileCollection, StandardCharsets.UTF_8);

            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode rootNode = (ObjectNode) objectMapper.readTree(fileCollectionString);
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

            byte[] updatedFileCollection = objectMapper.writeValueAsString(rootNode).getBytes(StandardCharsets.UTF_8);
            Source_Detail sourceDetail = sourceDetailRepository.findById(sourceDetailId)
                    .orElseThrow(() -> new RuntimeException("Source_Detail not found with ID: " + sourceDetailId));
            sourceDetail.setFileCollectionPostman(updatedFileCollection);
            sourceDetailRepository.save(sourceDetail);

            int maxRetries = 5;
            int retries = 0;
            while (retries < maxRetries) {
                sourceDetail = sourceDetailRepository.findById(sourceDetailId)
                        .orElseThrow(() -> new RuntimeException("Source_Detail not found with ID: " + sourceDetailId));

                if (sourceDetail.getFileCollectionPostman() != null) {
                    break;
                }
                Thread.sleep(500);
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

    public boolean startDocker() {
        try {

            String dockerDesktopPath = PathUtil.getDockerDesktopPath();

            Process process = new ProcessBuilder(
                    "cmd.exe", "/c", "start", "\"\"", "\"" + dockerDesktopPath + "\"").start();

            int waitTimeInSeconds = 120;
            int intervalInMilliseconds = 2000;
            int waited = 0;

            while (waited < waitTimeInSeconds * 1000) {
                Process checkProcess = new ProcessBuilder("docker", "info").start();
                int exitCode = checkProcess.waitFor();

                if (exitCode == 0) {
                    System.out.println("Docker is running.");
                    return true;
                }

                Thread.sleep(intervalInMilliseconds);
                waited += intervalInMilliseconds;
            }

            System.out.println("Docker failed to start within the timeout period.");
            return false;

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String runPostmanCollection(Long examPaperId) {
        Exam_Paper examPaper = examPaperRepository.findById(examPaperId).orElse(null);
        if (examPaper == null || examPaper.getFileCollectionPostman() == null) {
            System.err.println("Exam Paper or fileCollectionPostman not exits.");

            sseController.pushGradingProcess(0l, GradingStatusEnum.ERROR, LocalDateTime.now(), examPaperId);

            return null;
        }

        try {

            String tempFileName = generateRandomString(20) + ".json";
            Path tempFile = Files.createTempFile(tempFileName, "");

            Files.write(tempFile, examPaper.getFileCollectionPostman(), StandardOpenOption.WRITE);

            String newmanCmdPath = PathUtil.getNewmanCmdPath();

            ProcessBuilder processBuilder = new ProcessBuilder(newmanCmdPath, "run",
                    tempFile.toAbsolutePath().toString());

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            String result = new String(process.getInputStream().readAllBytes());
            String errorOutput = new String(process.getErrorStream().readAllBytes());

            if (!errorOutput.isEmpty()) {
                System.err.println("Error running Newman: " + errorOutput);
                return null;
            }

            Files.deleteIfExists(tempFile);

            return result;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String generateRandomString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            sb.append(characters.charAt(index));
        }
        return sb.toString();
    }

    private boolean comparePostmanResults(String postmanResult, Long examPaperId) {

        List<String> postmanOutputFunctions = Arrays.stream(postmanResult.split("\n"))
                .filter(line -> line.contains("→"))
                .map(line -> {
                    int arrowIndex = line.indexOf("→");
                    if (arrowIndex != -1 && arrowIndex + 1 < line.length()) {
                        return line.substring(arrowIndex + 2).trim();
                    }
                    return "";
                })
                .collect(Collectors.toList());

        List<Postman_For_Grading> postmanGradings = postmanForGradingRepository
                .findByExamPaper_ExamPaperIdAndStatusTrueOrderByOrderPriorityAsc(examPaperId);

        for (int i = 0; i < postmanGradings.size(); i++) {
            if (!postmanGradings.get(i).getOrderPriority().equals((long) (i + 1))) {
                System.err.println("Error comparePostmanResults");
                return false;
            }
        }

        List<String> postmanFunctionNames = postmanGradings.stream()
                .map(Postman_For_Grading::getPostmanFunctionName)
                .collect(Collectors.toList());

        return postmanOutputFunctions.equals(postmanFunctionNames);
    }

    public static Map.Entry<Path, String> findCsprojAndDotnetVersion(Path dirPath) throws IOException {
        Pattern pattern = Pattern.compile("<TargetFramework>(net\\d+\\.\\d+)</TargetFramework>");

        try (Stream<Path> folders = Files.walk(dirPath, 1)) {
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

}
