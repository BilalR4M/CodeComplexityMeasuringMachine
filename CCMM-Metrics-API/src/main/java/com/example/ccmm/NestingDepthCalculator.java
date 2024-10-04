package com.example.ccmm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
public class NestingDepthCalculator {

    private static final Logger LOGGER = LoggerFactory.getLogger(NestingDepthCalculator.class);

    @PostMapping("/calculate-loop-depth")
    public int calculateLoopDepth(@RequestBody CodeRequest codeRequest) {
        // Get the code from the request body
        String code = codeRequest.getCode();

        // Clean and prepare the input
        String cleanedCode = code.replaceAll("\\b(public|private|protected|static|final)\\b", "").trim();
        cleanedCode = cleanedCode.replaceAll("//.*", "");  // Remove single-line comments
        cleanedCode = cleanedCode.replaceAll("/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/", "");  // Remove block comments

        int currentDepth = 0; // Tracks the current loop depth
        int maxDepth = 0;     // Tracks the maximum nested depth
        int iteration = 0;

        LOGGER.info("Cleaned code: " + cleanedCode);

        // Iterate through each character since the code is a single line
        for (int i = 0; i < cleanedCode.length(); i++) {
            char currentChar = cleanedCode.charAt(i);

            // Detect loop keywords (for, while, do) by looking for them before an opening brace
            if (cleanedCode.startsWith("for", i) || cleanedCode.startsWith("while", i) || cleanedCode.startsWith("do", i)) {
                currentDepth++;
                LOGGER.info("Iteration: " + iteration++ + ", depth: " + currentDepth);
                maxDepth = Math.max(maxDepth, currentDepth); // Update max depth if necessary
            }

            // Decrease depth when a closing brace is found
            if (currentChar == '}') {
                currentDepth = Math.max(0, currentDepth - 1);
                LOGGER.info("Closing brace found, depth decreased to: " + currentDepth);
            }
        }

        LOGGER.info("Calculated loop depth: " + maxDepth); // Log the maximum nested depth
        return maxDepth;
    }
}



