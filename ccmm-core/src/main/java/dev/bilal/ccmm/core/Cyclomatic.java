package dev.bilal.ccmm.core;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.CompactConstructorDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class Cyclomatic {
    private static final Halstead UNMEASURED = new Halstead(0, 0, 0, 0, 0, 0, 0, 0, 0);

    private Cyclomatic() {}

    static List<MethodMetrics> methods(CompilationUnit unit) {
        List<MethodMetrics> methods = new ArrayList<>();
        for (MethodDeclaration method : unit.findAll(MethodDeclaration.class)) {
            methods.add(metrics(method));
        }
        for (ConstructorDeclaration constructor : unit.findAll(ConstructorDeclaration.class)) {
            methods.add(metrics(constructor));
        }
        methods.sort(Comparator.comparingInt(MethodMetrics::startLine).thenComparing(MethodMetrics::name));
        return List.copyOf(methods);
    }

    private static MethodMetrics metrics(CallableDeclaration<?> declaration) {
        int line = declaration.getBegin().map(position -> position.line).orElse(1);
        return new MethodMetrics(declaration.getNameAsString(), line, score(declaration), 0, UNMEASURED, 0);
    }

    private static int score(CallableDeclaration<?> declaration) {
        Counter counter = new Counter();
        declaration.accept(counter, null);
        return counter.score;
    }

    private static final class Counter extends VoidVisitorAdapter<Void> {
        private int score = 1;
        private boolean root = true;

        @Override
        public void visit(MethodDeclaration declaration, Void arg) {
            if (root) {
                root = false;
                super.visit(declaration, arg);
            }
        }

        @Override
        public void visit(ConstructorDeclaration declaration, Void arg) {
            if (root) {
                root = false;
                super.visit(declaration, arg);
            }
        }

        @Override
        public void visit(CompactConstructorDeclaration declaration, Void arg) {
            if (root) {
                root = false;
                super.visit(declaration, arg);
            }
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration declaration, Void arg) {}

        @Override
        public void visit(EnumDeclaration declaration, Void arg) {}

        @Override
        public void visit(RecordDeclaration declaration, Void arg) {}

        @Override
        public void visit(IfStmt statement, Void arg) {
            score++;
            super.visit(statement, arg);
        }

        @Override
        public void visit(ForStmt statement, Void arg) {
            score++;
            super.visit(statement, arg);
        }

        @Override
        public void visit(ForEachStmt statement, Void arg) {
            score++;
            super.visit(statement, arg);
        }

        @Override
        public void visit(WhileStmt statement, Void arg) {
            score++;
            super.visit(statement, arg);
        }

        @Override
        public void visit(DoStmt statement, Void arg) {
            score++;
            super.visit(statement, arg);
        }

        @Override
        public void visit(CatchClause clause, Void arg) {
            score++;
            super.visit(clause, arg);
        }

        @Override
        public void visit(SwitchEntry entry, Void arg) {
            if (!entry.isDefault()) {
                score += Math.max(1, entry.getLabels().size());
            }
            super.visit(entry, arg);
        }

        @Override
        public void visit(BinaryExpr expression, Void arg) {
            BinaryExpr.Operator operator = expression.getOperator();
            if (operator == BinaryExpr.Operator.AND || operator == BinaryExpr.Operator.OR) {
                score++;
            }
            super.visit(expression, arg);
        }

        @Override
        public void visit(ConditionalExpr expression, Void arg) {
            score++;
            super.visit(expression, arg);
        }
    }
}
