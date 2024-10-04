package com.example.ccmm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@CrossOrigin
public class HalsteadMetricsCalculator {

    private static final Logger LOGGER = LoggerFactory.getLogger(HalsteadMetricsCalculator.class);

    // Updated regex to better identify operators without overlap
    private static final String[] OPERATORS = {
            "\\+\\+", "--", "\\+=", "-=", "\\*=", "/=", "%=", "&=", "\\|=", "\\^=", "<<=", ">>=",  // Assignment operators
            "\\+", "-", "\\*", "/", "%",  // Arithmetic operators
            "=", "==", "!=", "<", ">", "<=", ">=",  // Comparison operators
            "\\|\\|", "&&", "!", "&", "\\|",  // Logical operators
            "\\^", "~", "<<", ">>"  // Bitwise operators
    };

    @PostMapping("/calculate-halstead")
    public HalsteadMetrics calculateHalstead(@RequestBody String code) {
        // Split code into lines
        String[] lines = code.split("\\r?\\n");

        HashSet<String> uniqueOperators = new HashSet<>();
        HashSet<String> uniqueOperands = new HashSet<>();
        int totalOperators = 0;
        int totalOperands = 0;

        for (String line : lines) {
            // Strip out comments
            String cleanedLine = line.replaceAll("//.*", "").replaceAll("/\\*.*?\\*/", "").trim();
            LOGGER.info("Cleaned code: " + cleanedLine);

            // Find operators in the line
            for (String operator : OPERATORS) {
                Pattern operatorPattern = Pattern.compile(operator);
                Matcher operatorMatcher = operatorPattern.matcher(cleanedLine);

                while (operatorMatcher.find()) {
                    uniqueOperators.add(operator); // Add operator to unique set
                    totalOperators++; // Count total operators
                }
            }

            // Find operands (identifiers and literals) in the line, excluding keywords
            Pattern operandPattern = Pattern.compile("\\b[A-Za-z_][A-Za-z0-9_]*\\b");
            Matcher operandMatcher = operandPattern.matcher(cleanedLine);

            while (operandMatcher.find()) {
                String operand = operandMatcher.group();
                // Exclude language keywords (e.g., int, if, for)
                if (!isKeyword(operand)) {
                    uniqueOperands.add(operand); // Add operand to unique set
                    totalOperands++; // Count total operands
                }
            }
        }

        int n1 = uniqueOperators.size();
        int n2 = uniqueOperands.size();
        int N1 = totalOperators;
        int N2 = totalOperands;

        // Calculate Halstead metrics
        int vocabulary = n1 + n2;
        int length = N1 + N2;
        double volume = length * (Math.log(vocabulary) / Math.log(2));
        double difficulty = (n1 / 2.0) * (N2 / (double) n2);
        double effort = difficulty * volume;

        LOGGER.info("Halstead Metrics calculated: Vocabulary = " + vocabulary + ", Length = " + length +
                ", Volume = " + volume + ", Difficulty = " + difficulty + ", Effort = " + effort);

        // Return a response with the calculated metrics
        return new HalsteadMetrics(vocabulary, length, volume, difficulty, effort);
    }

    // Method to check if a string is a language keyword
    private boolean isKeyword(String word) {
        String[] keywords = {
                "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue",
                "default", "do", "double", "else", "enum", "extends", "final", "finally", "float", "for", "goto", "if",
                "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "null", "package",
                "private", "protected", "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized",
                "this", "throw", "throws", "transient", "try", "void", "volatile", "while", "true", "false"
        };
        for (String keyword : keywords) {
            if (keyword.equals(word)) {
                return true;
            }
        }
        return false;
    }

    // A simple class to hold the Halstead metrics results
    public static class HalsteadMetrics {
        private int vocabulary;
        private int length;
        private double volume;
        private double difficulty;
        private double effort;

        public HalsteadMetrics(int vocabulary, int length, double volume, double difficulty, double effort) {
            this.vocabulary = vocabulary;
            this.length = length;
            this.volume = volume;
            this.difficulty = difficulty;
            this.effort = effort;
        }

        public int getVocabulary() {
            return vocabulary;
        }

        public int getLength() {
            return length;
        }

        public double getVolume() {
            return volume;
        }

        public double getDifficulty() {
            return difficulty;
        }

        public double getEffort() {
            return effort;
        }
    }
}



//package com.example.ccmt;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.web.bind.annotation.CrossOrigin;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RestController;
//import java.util.HashSet;
//import java.util.regex.Matcher;
//
//import java.util.regex.Pattern;
//
//@RestController
//@CrossOrigin
//public class HalsteadMetricsCalculator {
//
//    private static final Logger LOGGER = LoggerFactory.getLogger(HalsteadMetricsCalculator.class);
//
//    // Define basic operators for Halstead metric calculation
//    private static final String[] OPERATORS = {
//            "\\+", "-", "\\*", "/", "%", "=", "==", "!=", "<", ">", "<=", ">=", "\\|\\|", "&&", "\\!", "&", "\\|",
//            "\\^", "~", "<<", ">>", "\\+\\+", "--", "\\+=", "-=", "\\*=", "/=", "%=", "&=", "\\|=", "\\^=", "<<=", ">>="
//    };
//
//    @PostMapping("/calculate-halstead")
//    public HalsteadMetrics calculateHalstead(@RequestBody String code) {
//        // Split code into lines
//        String[] lines = code.split("\\r?\\n");
//
//        HashSet<String> uniqueOperators = new HashSet<>();
//        HashSet<String> uniqueOperands = new HashSet<>();
//        int totalOperators = 0;
//        int totalOperands = 0;
//
//        for (String line : lines) {
//            // Strip out comments
//            String cleanedLine = line.replaceAll("//.*", "").replaceAll("/\\*.*?\\*/", "").trim();
//            LOGGER.info("Cleaned code: " + cleanedLine);
//
//            // Find operators in the line
//            for (String operator : OPERATORS) {
//                Pattern operatorPattern = Pattern.compile(operator);
//                Matcher operatorMatcher = operatorPattern.matcher(cleanedLine);
//
//                while (operatorMatcher.find()) {
//                    uniqueOperators.add(operator); // Add operator to unique set
//                    totalOperators++; // Count total operators
//                }
//            }
//
//            // Find operands (simple identifiers and literals) in the line
//            Pattern operandPattern = Pattern.compile("\\b[A-Za-z_][A-Za-z0-9_]*\\b");
//            Matcher operandMatcher = operandPattern.matcher(cleanedLine);
//
//            while (operandMatcher.find()) {
//                String operand = operandMatcher.group();
//                uniqueOperands.add(operand); // Add operand to unique set
//                totalOperands++; // Count total operands
//            }
//        }
//
//        int n1 = uniqueOperators.size();
//        int n2 = uniqueOperands.size();
//        int N1 = totalOperators;
//        int N2 = totalOperands;
//
//        // Calculate Halstead metrics
//        int vocabulary = n1 + n2;
//        int length = N1 + N2;
//        double volume = length * (Math.log(vocabulary) / Math.log(2));
//        double difficulty = (n1 / 2.0) * (N2 / (double) n2);
//        double effort = difficulty * volume;
//
//        LOGGER.info("Halstead Metrics calculated: Vocabulary = " + vocabulary + ", Length = " + length +
//                ", Volume = " + volume + ", Difficulty = " + difficulty + ", Effort = " + effort);
//
//        // Return a response with the calculated metrics
//        return new HalsteadMetrics(vocabulary, length, volume, difficulty, effort);
//    }
//
//    // A simple class to hold the Halstead metrics results
//    public static class HalsteadMetrics {
//        private int vocabulary;
//        private int length;
//        private double volume;
//        private double difficulty;
//        private double effort;
//
//        public HalsteadMetrics(int vocabulary, int length, double volume, double difficulty, double effort) {
//            this.vocabulary = vocabulary;
//            this.length = length;
//            this.volume = volume;
//            this.difficulty = difficulty;
//            this.effort = effort;
//        }
//
//        public int getVocabulary() {
//            return vocabulary;
//        }
//
//        public int getLength() {
//            return length;
//        }
//
//        public double getVolume() {
//            return volume;
//        }
//
//        public double getDifficulty() {
//            return difficulty;
//        }
//
//        public double getEffort() {
//            return effort;
//        }
//    }
//}
