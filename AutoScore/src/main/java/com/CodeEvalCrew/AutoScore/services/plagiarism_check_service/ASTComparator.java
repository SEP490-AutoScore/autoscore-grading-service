package com.CodeEvalCrew.AutoScore.services.plagiarism_check_service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Service;

import com.CodeEvalCrew.AutoScore.models.Entity.Source_Detail;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

@Service
public class ASTComparator {

    List<CompilationUnit> studentASTList = new ArrayList<>();

    public void runPlagiarismDetection(List<Source_Detail> sourceDetails) {
        this.studentASTList = new ArrayList<>();
    }

    @SuppressWarnings("CallToPrintStackTrace")
    void createASTMatrix(List<Source_Detail> sourceDetails) {
        studentASTList.clear();
        System.out.println("Start creating ASTs for all students.");

        for (Source_Detail detail : sourceDetails) {
            try {
                CompilationUnit ast = parseFolderToAST(detail.getStudentSourceCodePath());
                if (ast != null) {
                    studentASTList.add(ast);
                }
            } catch (ParseProblemException | IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Complete AST generation for all students.");
    }

    // Hàm lấy các đoạn mã giống nhau
    @SuppressWarnings({"CallToPrintStackTrace", "UseSpecificCatch"})
    List<String> extractSimilarSections(Source_Detail detail1, Source_Detail detail2) {
        List<String> similarSections = new ArrayList<>();
        System.out.println("Extracting similar sections between student " + detail1.getStudent().getStudentId()
                + " and student " + detail2.getStudent().getStudentId());

        try {
            CompilationUnit ast1 = parseFolderToAST(detail1.getStudentSourceCodePath());
            CompilationUnit ast2 = parseFolderToAST(detail2.getStudentSourceCodePath());

            // Kiểm tra nếu cả hai AST đều không rỗng
            if (ast1 != null && ast2 != null) {
                System.out.println("Number of methods in ast1: " + ast1.findAll(MethodDeclaration.class).size());
                System.out.println("Number of methods in ast2: " + ast2.findAll(MethodDeclaration.class).size());

                // So sánh các phương thức
                ast1.findAll(MethodDeclaration.class).forEach(method1 -> {
                    ast2.findAll(MethodDeclaration.class).forEach(method2 -> {
                        if (method1.getBody().isPresent() && method2.getBody().isPresent()) {
                            String methodBody1 = method1.getBody().get().toString();
                            String methodBody2 = method2.getBody().get().toString();

                            // Kiểm tra độ tương đồng với ngưỡng 80%
                            if (isMethodSimilar(methodBody1, methodBody2, 0.8)) {
                                similarSections.add("Similar method found: " + method1.getNameAsString());
                            }
                        }
                    });
                });

                // So sánh các lớp
                ast1.findAll(ClassOrInterfaceDeclaration.class).forEach(class1 -> {
                    ast2.findAll(ClassOrInterfaceDeclaration.class).forEach(class2 -> {
                        Set<String> members1 = class1.getMembers().stream().map(Object::toString).collect(Collectors.toSet());
                        Set<String> members2 = class2.getMembers().stream().map(Object::toString).collect(Collectors.toSet());

                        // Kiểm tra nếu có thành viên giống nhau
                        if (!Collections.disjoint(members1, members2)) {
                            similarSections.add("Similar class found: " + class1.getNameAsString());
                        }
                    });
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Extracted " + similarSections.size() + " similar sections.");
        return similarSections;
    }

    // Hàm phụ để kiểm tra mức độ tương đồng giữa hai phương thức
    private boolean isMethodSimilar(String methodBody1, String methodBody2, double threshold) {
        int distance = LevenshteinDistance.getDefaultInstance().apply(methodBody1, methodBody2);
        int maxLength = Math.max(methodBody1.length(), methodBody2.length());
        double similarity = 1 - (double) distance / maxLength;
        return similarity >= threshold;
    }

    @SuppressWarnings("CallToPrintStackTrace")
    private CompilationUnit parseFolderToAST(String folderPath) throws IOException {
        StringBuilder combinedCode = new StringBuilder();

        Files.walk(Paths.get(folderPath))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".cs")) // Chỉ đọc các tệp .cs
                .forEach(path -> {
                    try {
                        String fileContent = new String(Files.readAllBytes(path));
                        // System.out.println("Content of file " + path + ":\n" + fileContent); // In nội dung tệp
                        combinedCode.append(fileContent).append("\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        // System.out.println("Combined code:\n" + combinedCode); // Kiểm tra nội dung kết hợp

        return new JavaParser().parse(combinedCode.toString()).getResult().orElse(null);
    }
}