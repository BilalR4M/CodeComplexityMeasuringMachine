package com.example.ccmm;

public class LineCountResponse {

    private int codeLines;
    private int commentLines;
    private int blankLines;
    private int functionCount; // New field to store the number of functions

    // Constructor
    public LineCountResponse(int codeLines, int commentLines, int blankLines, int functionCount) {
        this.codeLines = codeLines;
        this.commentLines = commentLines;
        this.blankLines = blankLines;
        this.functionCount = functionCount;
    }

    // Getters and setters
    public int getCodeLines() {
        return codeLines;
    }

    public void setCodeLines(int codeLines) {
        this.codeLines = codeLines;
    }

    public int getCommentLines() {
        return commentLines;
    }

    public void setCommentLines(int commentLines) {
        this.commentLines = commentLines;
    }

    public int getBlankLines() {
        return blankLines;
    }

    public void setBlankLines(int blankLines) {
        this.blankLines = blankLines;
    }

    public int getFunctionCount() {
        return functionCount;
    }

    public void setFunctionCount(int functionCount) {
        this.functionCount = functionCount;
    }
}


//package com.example.wcc;
//
//public class LineCountResponse {
//    private int codeLines;
//    private int commentLines;
//    private int blankLines;
//
//    public LineCountResponse(int lines, int codeLines, int commentLines, int blankLines) {
//        this.codeLines = codeLines;
//        this.commentLines = commentLines;
//        this.blankLines = blankLines;
//    }
//
//    public int getCodeLines() {
//        return codeLines;
//    }
//
//    public void setCodeLines(int codeLines) {
//        this.codeLines = codeLines;
//    }
//
//    public int getCommentLines() {
//        return commentLines;
//    }
//
//    public void setCommentLines(int commentLines) {
//        this.commentLines = commentLines;
//    }
//
//    public int getBlankLines() {
//        return blankLines;
//    }
//
//    public void setBlankLines(int blankLines) {
//        this.blankLines = blankLines;
//    }
//}
