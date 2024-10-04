package com.example.ccmm;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@CrossOrigin
public class MaintainabilityIndexCalculatorController {

    @PostMapping("/calculate-maintainability-index")
    public double[] calculateMaintainabilityIndex(@RequestBody String code) {
        // Step 1: Calculate Cyclomatic Complexity
        int cyclomaticComplexity = calculateCyclomaticComplexity(code);

        // Step 2: Calculate Lines of Code (LOC)
        int loc = calculateLinesOfCode(code);

        // Step 3: Calculate Halstead Volume
        double halsteadVolume = calculateHalsteadVolume(code);

        // Step 4: Compute Maintainability Index using the formula
        double maintainabilityIndex = (171 - 5.2 * Math.log(halsteadVolume)
                - 0.23 * cyclomaticComplexity
                - 16.2 * Math.log(loc)) * 100 / 171;

        // Ensure MI is non-negative
        maintainabilityIndex = Math.max(0, maintainabilityIndex);

        double[] array = {Math.round(maintainabilityIndex), cyclomaticComplexity, loc};

        return array;
    }

    private int calculateCyclomaticComplexity(String code) {
        int complexity = 1; // Base complexity is 1 for the default flow

        // Regular expressions to match decision points
        String[] decisionPoints = {
                "\\bif\\b", "\\belse\\b", "\\bfor\\b", "\\bwhile\\b", "\\bdo\\b", "\\bswitch\\b", "\\bcase\\b",
                "\\bcatch\\b", "\\bthrow\\b", "\\breturn\\b", "\\b&&\\b", "\\b\\|\\|\\b"
        };

        // Iterate through each decision point regex
        for (String point : decisionPoints) {
            Pattern pattern = Pattern.compile(point);
            Matcher matcher = pattern.matcher(code);

            // Increment complexity for each match
            while (matcher.find()) {
                complexity++;
            }
        }

        return complexity;
    }

    private int calculateLinesOfCode(String code) {
        String[] lines = code.split("\n");
        int nonEmptyLines = 0;

        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                nonEmptyLines++;
            }
        }

        return nonEmptyLines;
    }

    private double calculateHalsteadVolume(String code) {
        Set<String> operators = new HashSet<>();
        Set<String> operands = new HashSet<>();
        int N1 = 0, N2 = 0;

        // Define regex patterns for operators and operands
        String operatorRegex = "\\+|-|\\*|/|%|=|==|!=|<|>|<=|>=|&&|\\|\\||!|\\?|:|\\+\\+|--";
        String operandRegex = "\\b\\w+\\b";

        // Match operators
        Pattern operatorPattern = Pattern.compile(operatorRegex);
        Matcher operatorMatcher = operatorPattern.matcher(code);
        while (operatorMatcher.find()) {
            String operator = operatorMatcher.group();
            operators.add(operator);
            N1++;
        }

        // Match operands
        Pattern operandPattern = Pattern.compile(operandRegex);
        Matcher operandMatcher = operandPattern.matcher(code);
        while (operandMatcher.find()) {
            String operand = operandMatcher.group();
            operands.add(operand);
            N2++;
        }

        // Calculate Halstead Volume
        int eta1 = operators.size(); // Distinct operators
        int eta2 = operands.size();  // Distinct operands
        double volume = (N1 + N2) * (Math.log(eta1 + eta2) / Math.log(2));

        return volume;
    }
}
