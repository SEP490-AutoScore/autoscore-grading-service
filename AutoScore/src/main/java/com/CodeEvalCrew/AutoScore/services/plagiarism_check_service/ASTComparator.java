package com.CodeEvalCrew.AutoScore.services.plagiarism_check_service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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

            // Duyệt qua các phương thức
            if (ast1 != null && ast2 != null) {
                System.out.println("Number of methods in ast1: " + ast1.findAll(MethodDeclaration.class).size());
                System.out.println("Number of methods in ast2: " + ast2.findAll(MethodDeclaration.class).size());
                
                // So sánh các phương thức
                ast1.findAll(MethodDeclaration.class).forEach(method1 -> {
                    ast2.findAll(MethodDeclaration.class).forEach(method2 -> {
                        if (method1.getBody().isPresent() && method2.getBody().isPresent()
                                && method1.getBody().get().toString().equals(method2.getBody().get().toString())) {
                            similarSections.add("Similar method found: " + method1.getNameAsString());
                        }
                    });
                });

                // So sánh các lớp
                ast1.findAll(ClassOrInterfaceDeclaration.class).forEach(class1 -> {
                    ast2.findAll(ClassOrInterfaceDeclaration.class).forEach(class2 -> {
                        if (class1.getMembers().equals(class2.getMembers())) {
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

    @SuppressWarnings("CallToPrintStackTrace")
    private CompilationUnit parseFolderToAST(String folderPath) throws IOException {
        StringBuilder combinedCode = new StringBuilder();

        Files.walk(Paths.get(folderPath))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".cs")) // Lọc tệp .cs của .NET
                .forEach(path -> {
                    try {
                        combinedCode.append(new String(Files.readAllBytes(path))).append("\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        // Chuyển mã nguồn đã kết hợp thành AST
        return new JavaParser().parse(combinedCode.toString()).getResult().orElse(null);
    }
}
