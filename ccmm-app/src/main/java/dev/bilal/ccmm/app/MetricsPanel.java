package dev.bilal.ccmm.app;

import dev.bilal.ccmm.core.AnalysisResult;
import dev.bilal.ccmm.core.FileMetrics;
import dev.bilal.ccmm.core.MethodMetrics;
import java.util.Locale;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

final class MetricsPanel extends VBox {
    static final String STALE = "Stale";

    private final Label status = new Label();
    private final Label fileTitle = new Label("File");
    private final Label fileBody = new Label();
    private final Label methodTitle = new Label("Method");
    private final Label methodBody = new Label();
    private AnalysisResult.Ok shown;

    MetricsPanel() {
        setPrefWidth(240);
        status.setWrapText(true);
        fileBody.setWrapText(true);
        methodBody.setWrapText(true);
        getChildren().addAll(status, fileTitle, fileBody, methodTitle, methodBody);
        showUnparsed("");
    }

    void showFresh(AnalysisResult.Ok ok, int caretLine) {
        shown = ok;
        status.setText("");
        render(caretLine);
    }

    void showStale(AnalysisResult.Ok ok, int caretLine) {
        shown = ok;
        status.setText(STALE);
        render(caretLine);
    }

    void showUnparsed(String message) {
        shown = null;
        status.setText(message == null ? "" : message);
        setShown(fileTitle, false);
        setShown(fileBody, false);
        setShown(methodTitle, false);
        setShown(methodBody, false);
        fileBody.setText("");
        methodBody.setText("");
    }

    void caretMoved(int caretLine) {
        if (shown != null) {
            render(caretLine);
        }
    }

    String text() {
        return status.getText() + "\n" + fileBody.getText() + "\n" + methodBody.getText();
    }

    static MethodMetrics methodAt(FileMetrics file, int caretLine) {
        MethodMetrics found = null;
        for (MethodMetrics method : file.methods()) {
            if (method.startLine() > caretLine) {
                break;
            }
            found = method;
        }
        return found;
    }

    private void render(int caretLine) {
        setShown(fileTitle, true);
        setShown(fileBody, true);
        fileBody.setText(block(
                shown.file().maintainability(),
                shown.file().cyclomatic(),
                shown.file().codeLines(),
                shown.file().halstead().volume(),
                shown.file().maxLoopNesting()));
        MethodMetrics method = methodAt(shown.file(), caretLine);
        setShown(methodTitle, true);
        setShown(methodBody, true);
        methodBody.setText(method == null
                ? "No method"
                : method.name() + "\n" + block(
                        method.maintainability(),
                        method.cyclomatic(),
                        method.codeLines(),
                        method.halstead().volume(),
                        method.loopNesting()));
    }

    private static String block(double maintainability, int cyclomatic, int codeLines, double volume, int nesting) {
        return """
                Maintainability: %s
                Cyclomatic: %d
                Code lines: %d
                Volume: %s
                Nesting: %d
                """.formatted(number(maintainability), cyclomatic, codeLines, number(volume), nesting);
    }

    private static String number(double value) {
        return String.format(Locale.US, "%.1f", value);
    }

    private static void setShown(Label label, boolean shown) {
        label.setVisible(shown);
        label.setManaged(shown);
    }
}
