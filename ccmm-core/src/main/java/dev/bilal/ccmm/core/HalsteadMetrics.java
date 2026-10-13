package dev.bilal.ccmm.core;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.CompactConstructorDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SwitchExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TextBlockLiteralExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import java.util.HashSet;
import java.util.Set;

final class HalsteadMetrics {
    private HalsteadMetrics() {}

    static Halstead measure(Node node) {
        Counter counter = new Counter(node instanceof CallableDeclaration);
        node.accept(counter, null);
        return counter.toHalstead();
    }

    private static final class Counter extends VoidVisitorAdapter<Void> {
        private static final Set<String> KEYWORDS = Set.of(
                "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
                "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
                "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
                "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
                "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
                "volatile", "while", "var", "yield", "record", "sealed", "permits");

        private final boolean skipNested;
        private boolean atRoot = true;
        private final Set<String> operators = new HashSet<>();
        private final Set<String> operands = new HashSet<>();
        private int totalOperators;
        private int totalOperands;

        private Counter(boolean skipNested) {
            this.skipNested = skipNested;
        }

        private Halstead toHalstead() {
            int uniqueOperators = operators.size();
            int uniqueOperands = operands.size();
            int vocabulary = uniqueOperators + uniqueOperands;
            int length = totalOperators + totalOperands;
            double volume = vocabulary <= 1 ? 0 : length * (Math.log(vocabulary) / Math.log(2));
            double difficulty = uniqueOperands == 0 ? 0 : (uniqueOperators / 2.0) * ((double) totalOperands / uniqueOperands);
            double effort = difficulty * volume;
            return new Halstead(
                    uniqueOperators,
                    uniqueOperands,
                    totalOperators,
                    totalOperands,
                    vocabulary,
                    length,
                    volume,
                    difficulty,
                    effort);
        }

        private void operator(String token) {
            operators.add(token);
            totalOperators++;
        }

        private void operand(String token) {
            if (token == null || token.isBlank() || KEYWORDS.contains(token)) {
                return;
            }
            operands.add(token);
            totalOperands++;
        }

        private boolean enterCallable() {
            if (skipNested && !atRoot) {
                return false;
            }
            if (skipNested) {
                atRoot = false;
            }
            return true;
        }

        @Override
        public void visit(MethodDeclaration declaration, Void arg) {
            if (!enterCallable()) {
                return;
            }
            operand(declaration.getNameAsString());
            super.visit(declaration, arg);
        }

        @Override
        public void visit(ConstructorDeclaration declaration, Void arg) {
            if (!enterCallable()) {
                return;
            }
            operand(declaration.getNameAsString());
            super.visit(declaration, arg);
        }

        @Override
        public void visit(CompactConstructorDeclaration declaration, Void arg) {
            if (!enterCallable()) {
                return;
            }
            operand(declaration.getNameAsString());
            super.visit(declaration, arg);
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration declaration, Void arg) {
            if (skipNested && !atRoot) {
                return;
            }
            operand(declaration.getNameAsString());
            super.visit(declaration, arg);
        }

        @Override
        public void visit(EnumDeclaration declaration, Void arg) {
            if (skipNested && !atRoot) {
                return;
            }
            operand(declaration.getNameAsString());
            super.visit(declaration, arg);
        }

        @Override
        public void visit(RecordDeclaration declaration, Void arg) {
            if (skipNested && !atRoot) {
                return;
            }
            operand(declaration.getNameAsString());
            super.visit(declaration, arg);
        }

        @Override
        public void visit(Parameter parameter, Void arg) {
            operand(parameter.getNameAsString());
            super.visit(parameter, arg);
        }

        @Override
        public void visit(VariableDeclarator declarator, Void arg) {
            operand(declarator.getNameAsString());
            super.visit(declarator, arg);
        }

        @Override
        public void visit(EnumConstantDeclaration declaration, Void arg) {
            operand(declaration.getNameAsString());
            super.visit(declaration, arg);
        }

        @Override
        public void visit(ClassOrInterfaceType type, Void arg) {
            operand(type.getNameAsString());
            super.visit(type, arg);
        }

        @Override
        public void visit(NameExpr expression, Void arg) {
            operand(expression.getNameAsString());
            super.visit(expression, arg);
        }

        @Override
        public void visit(FieldAccessExpr expression, Void arg) {
            operator(".");
            operand(expression.getNameAsString());
            super.visit(expression, arg);
        }

        @Override
        public void visit(MethodCallExpr expression, Void arg) {
            operator("()");
            operand(expression.getNameAsString());
            super.visit(expression, arg);
        }

        @Override
        public void visit(MethodReferenceExpr expression, Void arg) {
            operator("::");
            operand(expression.getIdentifier());
            super.visit(expression, arg);
        }

        @Override
        public void visit(ObjectCreationExpr expression, Void arg) {
            operator("new");
            super.visit(expression, arg);
        }

        @Override
        public void visit(ArrayCreationExpr expression, Void arg) {
            operator("new");
            operator("[]");
            super.visit(expression, arg);
        }

        @Override
        public void visit(ArrayAccessExpr expression, Void arg) {
            operator("[]");
            super.visit(expression, arg);
        }

        @Override
        public void visit(CastExpr expression, Void arg) {
            operator("cast");
            super.visit(expression, arg);
        }

        @Override
        public void visit(InstanceOfExpr expression, Void arg) {
            operator("instanceof");
            super.visit(expression, arg);
        }

        @Override
        public void visit(BinaryExpr expression, Void arg) {
            operator(expression.getOperator().asString());
            super.visit(expression, arg);
        }

        @Override
        public void visit(UnaryExpr expression, Void arg) {
            operator(expression.getOperator().asString());
            super.visit(expression, arg);
        }

        @Override
        public void visit(AssignExpr expression, Void arg) {
            operator(expression.getOperator().asString());
            super.visit(expression, arg);
        }

        @Override
        public void visit(ConditionalExpr expression, Void arg) {
            operator("?");
            operator(":");
            super.visit(expression, arg);
        }

        @Override
        public void visit(LambdaExpr expression, Void arg) {
            operator("->");
            super.visit(expression, arg);
        }

        @Override
        public void visit(IntegerLiteralExpr expression, Void arg) {
            operand(expression.getValue());
            super.visit(expression, arg);
        }

        @Override
        public void visit(LongLiteralExpr expression, Void arg) {
            operand(expression.getValue());
            super.visit(expression, arg);
        }

        @Override
        public void visit(DoubleLiteralExpr expression, Void arg) {
            operand(expression.getValue());
            super.visit(expression, arg);
        }

        @Override
        public void visit(CharLiteralExpr expression, Void arg) {
            operand("'" + expression.getValue() + "'");
            super.visit(expression, arg);
        }

        @Override
        public void visit(StringLiteralExpr expression, Void arg) {
            operand("\"" + expression.getValue() + "\"");
            super.visit(expression, arg);
        }

        @Override
        public void visit(TextBlockLiteralExpr expression, Void arg) {
            operand("\"" + expression.getValue() + "\"");
            super.visit(expression, arg);
        }

        @Override
        public void visit(BooleanLiteralExpr expression, Void arg) {
            operand(Boolean.toString(expression.getValue()));
            super.visit(expression, arg);
        }

        @Override
        public void visit(NullLiteralExpr expression, Void arg) {
            operand("null");
            super.visit(expression, arg);
        }

        @Override
        public void visit(IfStmt statement, Void arg) {
            operator("if");
            if (statement.hasElseBranch()) {
                operator("else");
            }
            super.visit(statement, arg);
        }

        @Override
        public void visit(ForStmt statement, Void arg) {
            operator("for");
            super.visit(statement, arg);
        }

        @Override
        public void visit(ForEachStmt statement, Void arg) {
            operator("for");
            super.visit(statement, arg);
        }

        @Override
        public void visit(WhileStmt statement, Void arg) {
            operator("while");
            super.visit(statement, arg);
        }

        @Override
        public void visit(DoStmt statement, Void arg) {
            operator("do");
            super.visit(statement, arg);
        }

        @Override
        public void visit(SwitchStmt statement, Void arg) {
            operator("switch");
            super.visit(statement, arg);
        }

        @Override
        public void visit(SwitchExpr expression, Void arg) {
            operator("switch");
            super.visit(expression, arg);
        }

        @Override
        public void visit(SwitchEntry entry, Void arg) {
            operator(entry.isDefault() ? "default" : "case");
            super.visit(entry, arg);
        }

        @Override
        public void visit(CatchClause clause, Void arg) {
            operator("catch");
            super.visit(clause, arg);
        }

        @Override
        public void visit(TryStmt statement, Void arg) {
            operator("try");
            super.visit(statement, arg);
        }

        @Override
        public void visit(ReturnStmt statement, Void arg) {
            operator("return");
            super.visit(statement, arg);
        }

        @Override
        public void visit(ThrowStmt statement, Void arg) {
            operator("throw");
            super.visit(statement, arg);
        }

        @Override
        public void visit(BreakStmt statement, Void arg) {
            operator("break");
            super.visit(statement, arg);
        }

        @Override
        public void visit(ContinueStmt statement, Void arg) {
            operator("continue");
            super.visit(statement, arg);
        }
    }
}
