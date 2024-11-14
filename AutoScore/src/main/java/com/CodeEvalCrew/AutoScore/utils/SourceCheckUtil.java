package com.CodeEvalCrew.AutoScore.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.CodeEvalCrew.AutoScore.exceptions.NotFoundException;
import com.CodeEvalCrew.AutoScore.models.DTO.RequestDTO.Important.StudentSource;
import com.CodeEvalCrew.AutoScore.models.DTO.StudentSourceInfoDTO;
import com.CodeEvalCrew.AutoScore.models.Entity.Exam_Paper;
import com.CodeEvalCrew.AutoScore.models.Entity.Important;
import com.CodeEvalCrew.AutoScore.models.Entity.Score;
import com.CodeEvalCrew.AutoScore.models.Entity.Student;
import com.CodeEvalCrew.AutoScore.repositories.score_repository.ScoreRepository;
import com.CodeEvalCrew.AutoScore.repositories.student_repository.StudentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class SourceCheckUtil implements ISourceCheckUtil {

    private static final String SECTION_STRING = "ConnectionStrings";
    private static final String NODE_DATABASE_STRING = "DefaultConnection";
    private static final String APPSETTING_NAME = "appsettings.json";
    private static final int REQUIRE_PROJ = 2;

    @Autowired
    private ScoreRepository scoreRepository;
    @Autowired
    private StudentRepository studentRepository;

    @Override
    public List<StudentSourceInfoDTO> getImportantToCheck(List<Important> importants, List<StudentSource> students, Exam_Paper examPaper) throws Exception, NotFoundException {
        List<StudentSourceInfoDTO> result = new ArrayList<>();
        for (StudentSource student : students) {
            boolean isPass = true;
            String error = "";
            for (Important important : importants) {
                
                switch (important.getImportantCode()) {
                    case "SLN" -> {
                        String solutionName = examPaper.getExam().getExamCode() + "_" + examPaper.getExamPaperCode() + "_" + student.getStudent().getStudentCode();
                        Optional<String> SLNFlag = checkSolutionName(solutionName, student.getSourceDetail().getStudentSourceCodePath());
                        if (SLNFlag == null) {
                            isPass = false;
                            error += String.format("Solution with %s not found;", solutionName);
                        }
                    }
                    case "CNS" -> {
                        Optional<String> CNTFlag = checkConnectionStrings(student.getSourceDetail().getStudentSourceCodePath(), SECTION_STRING, NODE_DATABASE_STRING, APPSETTING_NAME);
                        if (CNTFlag == null) {
                            isPass = false;
                            error += "Connection String not found;";
                        }
                    }
                    case "SST" -> {
                        Optional<String> SSTFlag = checkSourceStructure(student.getSourceDetail().getStudentSourceCodePath(), REQUIRE_PROJ);
                        if (SSTFlag == null) {
                            isPass = false;
                            error += "Source structure not valid;";
                        }
                    }
                    default ->
                        throw new Exception("Important not found");
                }
            }

            if (isPass) {
                result.add(new StudentSourceInfoDTO(student.getSourceDetail().getSourceDetailId(), student.getStudent().getStudentId(), student.getSourceDetail().getStudentSourceCodePath()));
            } else {
                Score score = scoreRepository.findByStudentIdAndExamPaperId(student.getStudent().getStudentId(), examPaper.getExamPaperId());
                if (score == null) {
                    score = new Score();
                    Student stu = studentRepository.findById(student.getStudent().getStudentId()).get();
                    if (stu == null) {
                        
                    }
                    score.setStudent(stu);

                    Exam_Paper examPaperToSave = new Exam_Paper();
                    examPaperToSave.setExamPaperId(examPaper.getExamPaperId());
                    score.setExamPaper(examPaper);
                    score.setTotalScore(0.0f);
                    score.setGradedAt(LocalDateTime.now());
                    score.setReason(error);
                    scoreRepository.save(score);
                    error = "";
                } else {
                    score.setReason(error);
                    scoreRepository.save(score);
                    error = "";
                }
            }
        }
        return result;
    }

    public Optional<String> checkImportant(String sourcePath, String studentCode, String examPaperCode, String examCode) throws Exception {
        try {
            //check solution name
            String solutionName = examCode + "_" + examPaperCode + "_" + studentCode;
            checkSolutionName(solutionName, sourcePath);
            //check connection string
            checkConnectionStrings(sourcePath, "ConnectionStrings", "MyDB", "appsettings.json");
            //chheck source structure
            checkSourceStructure(sourcePath, 2);

        } catch (NotFoundException e) {
            return Optional.empty();
        } catch (Exception e) {
            throw e;
        }

        return Optional.empty();
    }

    private Optional<String> checkConnectionStrings(String sourcePath, String section, String dbNode, String fileName) throws Exception, NotFoundException {
        Optional<String> jsonPath = findAppsettingsJsonPath(sourcePath);
        if (jsonPath.isEmpty()) {
            throw new NotFoundException(fileName + " not found");
        }
        boolean flag = analyzeAppSettings(jsonPath.get(), section, dbNode);
        if (flag) {
            return Optional.of("PASS");
        }
        return null;
    }

    private Optional<String> checkSolutionName(String solutionName, String sourcePath) throws NotFoundException {
        try {
            Optional<String> solution = findSolutionName(sourcePath, solutionName);
            if (solution.isEmpty()) {
                return null;
            }
            return solution;
        } catch (Exception e) {
            System.out.println(e.getCause());
            throw e;
        }
    }

    private static Optional<String> checkSourceStructure(String projectRoot, int requireProj) throws Exception {
        List<File> csprojFiles = collectCsprojFiles(new File(projectRoot));
        if (csprojFiles.isEmpty()) {
            System.out.println("No .csproj files found.");
            return null;
        }
        if (csprojFiles.size() < requireProj) {
            System.out.println(".csproj files found does not meet the requirements.");
            return null;
        }
        File mainProject = findMainProject(csprojFiles);

        boolean allReferenced = checkAllProjectsReferenced(csprojFiles, mainProject);
        System.out.println(allReferenced ? "All non-main projects are referenced." : "Some non-main projects are not referenced by others.");
        if (allReferenced) {
            return Optional.of("PASS");
        }
        return null;
    }

    public static void analyzeCSharpStructure(String directoryPath) throws IOException {
        try {
            Files.walk(Paths.get(directoryPath))
                    .filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".cs")) // Only C# source files
                    .forEach(file -> {
                        try {
                            analyzeFile(file.toFile());
                        } catch (IOException e) {
                            System.err.println("Error reading file: " + file);
                        }
                    });
        } catch (IOException e) {
            System.err.println("Error walking through directory: " + directoryPath);
        }
    }

    private static void analyzeFile(File file) throws IOException {
        System.out.println("Analyzing file: " + file.getName());
        List<String> lines = Files.readAllLines(file.toPath());

        for (String line : lines) {
            checkForNamespace(line);
            checkForClass(line);
            checkForMethod(line);
        }
    }

    private static void checkForNamespace(String line) {
        Pattern namespacePattern = Pattern.compile("\\bnamespace\\s+([a-zA-Z_][a-zA-Z0-9_\\.]*)");
        Matcher matcher = namespacePattern.matcher(line);
        if (matcher.find()) {
            System.out.println("Namespace found: " + matcher.group(1));
            System.out.println("");
        }
    }

    private static void checkForClass(String line) {
        Pattern classPattern = Pattern.compile("\\bclass\\s+([a-zA-Z_][a-zA-Z0-9_]*)");
        Matcher matcher = classPattern.matcher(line);
        if (matcher.find()) {
            System.out.println("Class found: " + matcher.group(1));
            System.out.println("");
        }
    }

    private static void checkForMethod(String line) {
        Pattern methodPattern = Pattern.compile("\\b(public|private|protected|internal)?\\s+\\w+\\s+(\\w+)\\s*\\(");
        Matcher matcher = methodPattern.matcher(line);
        if (matcher.find()) {
            System.out.println("Method found: " + matcher.group(2));
            System.out.println("");
        }
    }

    public static Optional<String> findSolutionName(String directoryPath, String solutionName) {
        try {
            // Search for .sln files that contain the specified string in their names
            return Files.walk(Paths.get(directoryPath), 1)
                    .filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".sln"))
                    .filter(file -> file.getFileName().toString().contains(solutionName)) // Match the string part
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .map(fileName -> fileName.replace(".sln", "")) // Remove the .sln extension
                    .findFirst();

        } catch (IOException e) {
            return Optional.empty();
        }
    }

    //read appsetting
    public static boolean analyzeAppSettings(String filePath, String sectionName, String dbNode) {
        ObjectMapper mapper = new ObjectMapper();
        boolean result = false;
        try {
            // Parse the JSON file
            JsonNode rootNode = mapper.readTree(new File(filePath));

            // Check for known sections, e.g., "ConnectionStrings"
            if (rootNode.has(sectionName)) {
                System.out.println(sectionName + " section found.");
                JsonNode sectionNode = rootNode.get(sectionName);
                result = true;
                // // Check if the ConnectionStrings section has a DefaultConnection node
                // if (sectionName.equals(sectionName) && sectionNode.has(dbNode)) {
                //     System.out.println(dbNode + " found in " + sectionName + ".");
                //     displaySection(sectionNode, sectionName);
                // } else if (sectionName.equals(sectionName)) {
                //     System.out.println("No " + dbNode + " found in " + sectionName + ".");
                // }
            } else {
                System.out.println("No " + sectionName + " section found.");
            }

            // Add checks for other known sections as needed
            // Display all root keys
            System.out.println("Root Keys in appsettings.json:");
            Iterator<String> rootKeys = rootNode.fieldNames();
            while (rootKeys.hasNext()) {
                System.out.println(" - " + rootKeys.next());
            }

        } catch (IOException e) {
            System.err.println("Error reading or parsing file: " + filePath);
        }
        return result;
    }

    // Display nested JSON sections
    private static void displaySection(JsonNode sectionNode, String sectionName) {
        System.out.println("Contents of section: " + sectionName);
        Iterator<Entry<String, JsonNode>> fields = sectionNode.fields();
        while (fields.hasNext()) {
            Entry<String, JsonNode> field = fields.next();
            System.out.println(" - " + field.getKey() + ": " + field.getValue());
        }
    }

    //find appsettign func
    public static Optional<String> findAppsettingsJsonPath(String directoryPath) {
        try {
            // Search for the appsettings.json file in the specified directory and its subdirectories
            return Files.walk(Paths.get(directoryPath))
                    .filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().equals("appsettings.json")) // Find appsettings.json
                    .map(Path::toString) // Convert Path to String
                    .findFirst();                              // Return the first matching file path as String

        } catch (IOException e) {
            System.err.println("Error searching directory: " + directoryPath);
            return Optional.empty();
        }
    }

    // Function to get all folder names in the directory containing a .sln file
    public static List<String> getAllFolderNamesInSolutionDirectory(String directoryPath) {
        File solutionFolder = findSolutionFolder(directoryPath);
        List<String> folderNames = new ArrayList<>();

        if (solutionFolder != null) {
            // List all subdirectories in the solution folder
            File[] subdirectories = solutionFolder.listFiles(File::isDirectory);
            if (subdirectories != null) {
                for (File subdirectory : subdirectories) {
                    folderNames.add(subdirectory.getName()); // Get only the folder name
                }
            }
        }
        return folderNames;
    }

    // Helper function to find the folder containing the .sln file
    private static File findSolutionFolder(String directoryPath) {
        try {
            return Files.walk(Paths.get(directoryPath))
                    .filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".sln"))
                    .map(Path::getParent)
                    .findFirst()
                    .map(Path::toFile)
                    .orElse(null);

        } catch (IOException e) {
            System.err.println("Error searching for .sln file: " + e.getMessage());
            return null;
        }
    }

    // Helper function to recursively collect all .csproj files
    private static List<File> collectCsprojFiles(File dir) {
        List<File> csprojFiles = new ArrayList<>();
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                csprojFiles.addAll(collectCsprojFiles(file));
            } else if (file.getName().endsWith(".csproj")) {
                csprojFiles.add(file);
            }
        }
        return csprojFiles;
    }

    //main check
    // public static void main(String[] args) throws IOException {
    //     String projectPath = "C:\\Project\\PE_PRN231_SU24_009909\\StudentSolution\\1\\vuongvtse160599\\0\\PEPRN231_SU24_009909_VoTrongVuong_BE";  // Replace with the path to your project directory
    //     List<File> csprojFiles = collectCsprojFiles(new File(projectPath));
    //     if (csprojFiles.isEmpty()) {
    //         System.out.println("No .csproj files found.");
    //     } else {
    //         System.out.println(csprojFiles.size() + " .csproj files found:");
    //         // csprojFiles.forEach(file -> System.out.println(file.getAbsolutePath()));
    //     }
    //     File mainProject = findMainProject(csprojFiles);

    //     boolean allReferenced = checkAllProjectsReferenced(csprojFiles, mainProject);
    //     System.out.println(allReferenced ? "All non-main projects are referenced." : "Some non-main projects are not referenced by others.");
    // }

    static boolean checkAllProjectsReferenced(List<File> csprojFiles, File mainProject) {
        Map<String, List<String>> projectReferences = new HashMap<>();

        // Get project names and their references
        for (File csprojFile : csprojFiles) {
            String projectName = csprojFile.getName().replace(".csproj", "");
            List<String> references = getProjectReferences(csprojFile);
            projectReferences.put(projectName, references);
        }

        // Check if each project (excluding the main project) is referenced by at least one other project
        for (String project : projectReferences.keySet()) {
            if (mainProject.getName().replace(".csproj", "").equals(project)) {
                continue; // Skip the main project
            }

            boolean isReferenced = false;

            for (Map.Entry<String, List<String>> entry : projectReferences.entrySet()) {
                String otherProject = entry.getKey();
                List<String> references = entry.getValue();

                // Check if the current project is listed in the references of any other project
                if (!otherProject.equals(project) && references.contains(project)) {
                    isReferenced = true;
                    break;
                }
            }

            if (!isReferenced) {
                return false; // If any project is not referenced by another project
            }
        }

        return true; // All non-main projects are referenced by at least one other project
    }

    static File findMainProject(List<File> csprojFiles) {
        for (File csprojFile : csprojFiles) {
            File projectDir = csprojFile.getParentFile();
            File programFile = new File(projectDir, "Program.cs");

            // Check if Program.cs exists in the project directory
            if (programFile.exists()) {
                return csprojFile; // Return the project file if Program.cs is found
            }
        }
        return null; // No main project found
    }

    static List<String> getProjectReferences(File projectFile) {
        List<String> references = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(projectFile);
            NodeList nodeList = doc.getElementsByTagName("ProjectReference");
            for (int i = 0; i < nodeList.getLength(); i++) {
                String projectRef = nodeList.item(i).getAttributes().getNamedItem("Include").getNodeValue();
                String projectName = new File(projectRef).getName().replace(".csproj", "");
                references.add(projectName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return references;
    }
}
