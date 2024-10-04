package com.example.ccmm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
public class LineCountCalculator {

    private static final Logger LOGGER = LoggerFactory.getLogger(LineCountCalculator.class);

    @PostMapping("/calculate-line-count")
    public LineCountResponse calculateLineCount(@RequestBody CodeRequest codeRequest) {
        // Get the code from the request body
        String code = codeRequest.getCode();

        // Split the code into lines
        String[] lines = code.split("\n");

        int codeLines = 0;
        int commentLines = 0;
        int blankLines = 0;
        int functionCount = 0;

        boolean inBlockComment = false; // To handle multi-line comments
        boolean inFunctionDeclaration = false; // To detect multi-line method signatures

        for (String line : lines) {
            String trimmedLine = line.trim(); // Remove leading and trailing spaces

            // Handle blank lines
            if (trimmedLine.isEmpty()) {
                blankLines++;
                LOGGER.info("Blank line detected.");
                continue;
            }

            // Handle block comments
            if (inBlockComment || trimmedLine.startsWith("/*")) {
                commentLines++;
                LOGGER.info("Comment line detected.");
                if (trimmedLine.endsWith("*/")) {
                    inBlockComment = false; // End of block comment
                    LOGGER.info("End of block comment.");
                } else {
                    inBlockComment = true; // Inside a block comment
                }
                continue;
            }

            // Handle single-line comments
            if (trimmedLine.startsWith("//")) {
                commentLines++;
                LOGGER.info("Single-line comment detected.");
                continue;
            }

            // Detect function/method signature (possibly multi-line)
            if (inFunctionDeclaration) {
                // Continue until the closing parenthesis ')' and the opening brace '{' is found
                if (trimmedLine.contains("{")) {
                    functionCount++;
                    inFunctionDeclaration = false; // End of multi-line function declaration
                    LOGGER.info("End of multi-line function declaration. Incrementing function count.");
                }
                codeLines++; // Count lines inside function declaration
                continue;
            }

            // Start function detection if it contains a return type or modifier
            boolean hasReturnTypeOrModifier = false;
            boolean hasOpenParenthesis = false;

            // Split the line into words (to check for return types and method names)
            String[] words = trimmedLine.split("\\s+");

            // Check for valid return types and access modifiers
            String[] validModifiers = { "public", "private", "protected", "static", "final", "void", "int", "double", "float", "char", "boolean", "String", "List", "Map" };

            // Loop through words to check for return types or access modifiers
            for (String word : words) {
                for (String modifier : validModifiers) {
                    if (word.equals(modifier)) {
                        hasReturnTypeOrModifier = true;
                        LOGGER.info("Found return type or modifier: " + word);
                        break;
                    }
                }

                // Check if the word contains the opening parenthesis '(' (method declaration start)
                if (word.contains("(")) {
                    hasOpenParenthesis = true;
                    LOGGER.info("Found opening parenthesis: " + word);
                    break;
                }
            }

            // Detect the start of a function/method declaration
            if (hasReturnTypeOrModifier && hasOpenParenthesis) {
                // Check if the method declaration spans multiple lines
                if (!trimmedLine.contains("{")) {
                    inFunctionDeclaration = true; // Start of a multi-line function declaration
                    LOGGER.info("Start of a multi-line function declaration.");
                } else {
                    // If function signature is single-line, immediately count the function
                    functionCount++;
                    LOGGER.info("Function detected. Incrementing function count.");
                }
                codeLines++; // A function declaration is also considered a code line
                continue;
            }

            // Normal code lines (not a method signature or comment)
            codeLines++;
            LOGGER.info("Code line detected.");
        }

        LOGGER.info("Code Lines: " + codeLines);
        LOGGER.info("Comment Lines: " + commentLines);
        LOGGER.info("Blank Lines: " + blankLines);
        LOGGER.info("Function/Method Count: " + functionCount);

        return new LineCountResponse(codeLines, commentLines, blankLines, functionCount);
    }
}



//package com.example.wcc;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.web.bind.annotation.CrossOrigin;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@CrossOrigin
//public class LineCountCalculator {
//
//    private static final Logger LOGGER = LoggerFactory.getLogger(LineCountCalculator.class);
//
//    @PostMapping("/calculate-line-count")
//    public LineCountResponse calculateLineCount(@RequestBody CodeRequest codeRequest) {
//        // Get the code from the request body
//        String code = codeRequest.getCode();
//
//        // Clean and prepare the input
//        String[] lines = code.split("\n");
//
//        int codeLines = 0;
//        int commentLines = 0;
//        int blankLines = 0;
//
//        boolean inBlockComment = false; // To handle multi-line comments
//
//        for (String line : lines) {
//            String trimmedLine = line.trim(); // Remove leading and trailing spaces
//
//            if (trimmedLine.isEmpty()) {
//                blankLines++;
//                LOGGER.info("Blank line detected.");
//            } else if (inBlockComment || trimmedLine.startsWith("/*")) {
//                commentLines++;
//                LOGGER.info("Comment line detected.");
//                if (trimmedLine.endsWith("*/")) {
//                    inBlockComment = false; // End of block comment
//                    LOGGER.info("End of block comment.");
//                } else {
//                    inBlockComment = true; // Inside a block comment
//                }
//            } else if (trimmedLine.startsWith("//")) {
//                commentLines++;
//                LOGGER.info("Single-line comment detected.");
//            } else {
//                codeLines++;
//                LOGGER.info("Code line detected.");
//            }
//        }
//
//        LOGGER.info("Code Lines: {}", codeLines);
//        LOGGER.info("Comment Lines: {}", commentLines);
//        LOGGER.info("Blank Lines: {}", blankLines);
//
//        return new LineCountResponse(codeLines, commentLines, blankLines);
//    }
//}
