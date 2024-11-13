package com.CodeEvalCrew.AutoScore.services.plagiarism_check_service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.springframework.stereotype.Service;

import com.CodeEvalCrew.AutoScore.antlr4.CSharpLexer;
import com.CodeEvalCrew.AutoScore.antlr4.CSharpParser;
import com.CodeEvalCrew.AutoScore.antlr4.CSharpParser.Add_accessor_declarationContext;
import com.CodeEvalCrew.AutoScore.models.Entity.Source_Detail;

@Service
public class ASTComparator {

    /**
     * So sánh mã nguồn của hai sinh viên, chia mã theo từng tầng (Presentation, Business, DataAccess)
     * để phát hiện sự tương đồng trong từng tầng.
     */
    @SuppressWarnings("CallToPrintStackTrace")
    public List<String> compareLayers(Source_Detail detail1, Source_Detail detail2) {
        List<String> similarSections = new ArrayList<>();

        // So sánh từng tầng
        for (String layer : List.of("Presentation", "Business", "DataAccess", "DAO", "DTO", "Service")) {
            System.out.println("Comparing layer: " + layer);

            try {
                ParseTree ast1 = parseLayerToAST(detail1.getStudentSourceCodePath(), layer);
                ParseTree ast2 = parseLayerToAST(detail2.getStudentSourceCodePath(), layer);

                if (ast1 != null && ast2 != null) {
                    extractSimilarNodes(ast1, ast2, similarSections);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Total similar sections found: " + similarSections.size());
        return similarSections;
    }

    /**
     * Phân tích mã nguồn trong thư mục để tạo AST cho tầng cụ thể (layer).
     */
    @SuppressWarnings("CallToPrintStackTrace")
    private ParseTree parseLayerToAST(String folderPath, String layer) throws IOException {
        StringBuilder combinedCode = new StringBuilder();

        Files.walk(Paths.get(folderPath))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".cs"))
                .forEach(path -> {
                    // Chỉ đọc các tệp mã nguồn thuộc tầng hiện tại
                    if (path.toString().contains(layer)) {
                        try {
                            String fileContent = new String(Files.readAllBytes(path));
                            combinedCode.append(fileContent).append("\n");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });

        CSharpLexer lexer = new CSharpLexer(CharStreams.fromString(combinedCode.toString()));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        CSharpParser parser = new CSharpParser(tokens);

        return parser.compilation_unit(); // Trả về ParseTree từ parser
    }

    /**
     * So sánh các node trong hai AST và lưu các đoạn mã tương tự vào danh sách.
     */
    private void extractSimilarNodes(ParseTree studentNode, ParseTree dbNode, List<String> similarSections) {
        // Điều kiện dừng nếu node hiện tại không trùng khớp
        if (studentNode == null || dbNode == null) return;

        // So sánh node hiện tại
        if (studentNode.getText().equals(dbNode.getText())) {
            similarSections.add(studentNode.getText());
        }

        // Đệ quy để kiểm tra các con của node
        for (int i = 0; i < Math.min(studentNode.getChildCount(), dbNode.getChildCount()); i++) {
            extractSimilarNodes(studentNode.getChild(i), dbNode.getChild(i), similarSections);
        }
    }

    /**
     * Đếm tổng số node trong một AST để phục vụ tính toán tỷ lệ tương đồng.
     */
    @SuppressWarnings("CallToPrintStackTrace")
    public int getTotalNodes(Source_Detail sourceDetail) {
        try {
            ParseTree ast = parseFolderToAST(sourceDetail.getStudentSourceCodePath());
            return countNodes(ast);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Đếm số node trong AST.
     */
    private int countNodes(ParseTree tree) {
        if (tree == null) return 0;
        int count = 1; // Đếm node hiện tại
        for (int i = 0; i < tree.getChildCount(); i++) {
            count += countNodes(tree.getChild(i));
        }
        return count;
    }

    /**
     * Hàm phụ để phân tích cú pháp mã nguồn từ thư mục, không phân chia theo tầng.
     */
    @SuppressWarnings("CallToPrintStackTrace")
    private ParseTree parseFolderToAST(String folderPath) throws IOException {
        StringBuilder combinedCode = new StringBuilder();

        Files.walk(Paths.get(folderPath))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".cs"))
                .forEach(path -> {
                    try {
                        String fileContent = new String(Files.readAllBytes(path));
                        combinedCode.append(fileContent).append("\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        CSharpLexer lexer = new CSharpLexer(CharStreams.fromString(combinedCode.toString()));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        CSharpParser parser = new CSharpParser(tokens);

        return parser.compilation_unit();
    }

    /**
     * Lấy các node cụ thể trong AST như tên lớp, tên hàm, hoặc khai báo biến để so sánh chi tiết hơn.
     */
    private void extractSpecificNodes(ParseTree node, List<String> sections, String type) {
        if (node == null) return;

        // Lọc node theo loại (class, method, variable)
        if (type.equals("class") && node instanceof CSharpParser.Class_definitionContext) {
            sections.add(node.getText());
        } else if (type.equals("method") && node instanceof CSharpParser.Method_declarationContext) {
            sections.add(node.getText());
        } else if (type.equals("variable") && node instanceof Add_accessor_declarationContext) {
            sections.add(node.getText());
        }

        // Đệ quy duyệt các node con
        for (int i = 0; i < node.getChildCount(); i++) {
            extractSpecificNodes(node.getChild(i), sections, type);
        }
    }
}
