// package com.CodeEvalCrew.AutoScore.services.plagiarism_check_service;
// import org.antlr.v4.runtime.CharStreams;
// import org.antlr.v4.runtime.CommonTokenStream;
// import org.antlr.v4.runtime.tree.ParseTree;
// import org.antlr.v4.runtime.tree.ParseTreeWalker;
// public class CSharpASTParser {

//     public void parseAndExtractMethods(String code) {
//         // Khởi tạo lexer và parser
//         CSharpLexer lexer = new CSharpLexer(CharStreams.fromString(code));
//         CommonTokenStream tokens = new CommonTokenStream(lexer);
//         CSharpParser parser = new CSharpParser(tokens);

//         // Tạo cây cú pháp (parse tree) từ parser
//         ParseTree tree = parser.compilation_unit();

//         // Sử dụng ParseTreeWalker để duyệt cây cú pháp và trích xuất các phương thức
//         ParseTreeWalker walker = new ParseTreeWalker();
//         CSharpMethodListener listener = new CSharpMethodListener();
//         walker.walk(listener, tree);
//     }
// }

