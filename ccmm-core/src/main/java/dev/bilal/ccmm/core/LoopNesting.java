package dev.bilal.ccmm.core;

import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.CompactConstructorDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

final class LoopNesting {
    private LoopNesting() {}

    static int depth(CallableDeclaration<?> declaration) {
        Counter counter = new Counter();
        declaration.accept(counter, null);
        return counter.max;
    }

    private static final class Counter extends VoidVisitorAdapter<Void> {
        private int current;
        private int max;
        private boolean root = true;

        private void enter(Runnable walk) {
            current++;
            max = Math.max(max, current);
            walk.run();
            current--;
        }

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
        public void visit(ForStmt statement, Void arg) {
            enter(() -> super.visit(statement, arg));
        }

        @Override
        public void visit(ForEachStmt statement, Void arg) {
            enter(() -> super.visit(statement, arg));
        }

        @Override
        public void visit(WhileStmt statement, Void arg) {
            enter(() -> super.visit(statement, arg));
        }

        @Override
        public void visit(DoStmt statement, Void arg) {
            enter(() -> super.visit(statement, arg));
        }
    }
}
