package dev.bilal.ccmm.spikes.e0t04;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import javax.imageio.ImageIO;

/**
 * Captures dark and light token scenes at 100%, 150%, and 200%.
 * Run: {@code mvn -f spikes/e0-t04 package exec:java}
 */
public final class HidpiBakeoff {
    private static final String COMMAND = "mvn -f spikes/e0-t04 package exec:java";
    private static final double[] SCALES = {1.0, 1.5, 2.0};

    private HidpiBakeoff() {}

    public static void main(String[] args) throws Exception {
        TokenCheck.Result tokens = TokenCheck.verify();
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<List<String>> notes = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Runnable start = () -> capture(done, notes, failure);
        try {
            Platform.startup(start);
        } catch (IllegalStateException already) {
            Platform.runLater(start);
        }
        if (!done.await(5, TimeUnit.MINUTES)) {
            throw new IllegalStateException("HiDPI bake-off timed out");
        }
        if (failure.get() != null) {
            throw new Exception(failure.get());
        }

        Path outDir = reportDir();
        Files.createDirectories(outDir);
        boolean clipped = notes.get().stream().anyMatch(line -> line.contains("CLIPPED"));
        String decision = tokens.ok() && !clipped ? "go" : "no-go";
        Path report = outDir.resolve("hidpi.md");
        Files.writeString(report, format(tokens, notes.get(), decision));
        System.out.println(report.toAbsolutePath());
        System.out.println(decision);
        Platform.exit();
    }

    private static void capture(
            CountDownLatch done, AtomicReference<List<String>> notes, AtomicReference<Throwable> failure) {
        try {
            Platform.setImplicitExit(false);
            List<String> lines = new ArrayList<>();
            Path outDir = reportDir();
            Files.createDirectories(outDir);
            for (String theme : List.of("dark", "light")) {
                for (double scale : SCALES) {
                    lines.add(captureOne(theme, scale, outDir));
                }
            }
            notes.set(lines);
            done.countDown();
        } catch (Throwable error) {
            failure.set(error);
            done.countDown();
        }
    }

    private static String captureOne(String theme, double scale, Path outDir) throws Exception {
        Stage stage = new Stage();
        VBox root = buildScene();
        Scene scene = new Scene(root, 420, 220);
        String css = HidpiBakeoff.class.getResource("/" + theme + ".css").toExternalForm();
        scene.getStylesheets().add(css);
        Scale transform = new Scale(scale, scale);
        root.getTransforms().add(transform);
        stage.setScene(scene);
        stage.show();
        stage.sizeToScene();

        HBox treeRow = (HBox) root.lookup(".tree-row");
        Label primaryInRow = (Label) treeRow.getChildren().get(0);
        Bounds rowBounds = treeRow.getLayoutBounds();
        Bounds textBounds = primaryInRow.getBoundsInParent();
        boolean clipped = textBounds.getMaxY() > rowBounds.getHeight() + 0.5
                || primaryInRow.getLayoutBounds().getHeight() > 24.5;

        WritableImage image = new WritableImage(
                (int) Math.ceil(scene.getWidth() * scale), (int) Math.ceil(scene.getHeight() * scale));
        root.snapshot(null, image);
        String percent = percentName(scale);
        String fileName = "hidpi-" + theme + "-" + percent + ".png";
        Path file = outDir.resolve(fileName);
        ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file.toFile());
        stage.close();

        String clipNote = clipped ? "CLIPPED" : "not clipped";
        return theme + " @" + percent + ": " + fileName + "; tree row 24 px " + clipNote;
    }

    private static VBox buildScene() {
        Label primary = new Label("text-primary — The quick brown fox");
        primary.getStyleClass().add("text-primary");
        Label secondary = new Label("text-secondary — supporting line");
        secondary.getStyleClass().add("text-secondary");
        Label success = new Label("success band");
        success.getStyleClass().add("band-success");
        Label warn = new Label("warn band");
        warn.getStyleClass().add("band-warn");
        Label danger = new Label("danger band");
        danger.getStyleClass().add("band-danger");

        Label rowText = new Label("tree row primary");
        rowText.getStyleClass().add("text-primary");
        Label chip = new Label("Ada");
        chip.getStyleClass().add("remote-chip");
        HBox treeRow = new HBox(rowText, chip);
        treeRow.getStyleClass().add("tree-row");

        VBox panel = new VBox(primary, secondary, success, warn, danger, treeRow);
        panel.getStyleClass().add("panel");
        return panel;
    }

    private static String percentName(double scale) {
        return String.format(Locale.US, "%d", Math.round(scale * 100));
    }

    private static Path reportDir() {
        Path cwd = Path.of("").toAbsolutePath();
        if (Files.isDirectory(cwd.resolve("docs/benchmarks"))) {
            return cwd.resolve("docs/benchmarks");
        }
        if (Files.isDirectory(cwd.resolve("../../docs/benchmarks"))) {
            return cwd.resolve("../../docs/benchmarks").normalize();
        }
        return cwd.resolve("docs/benchmarks");
    }

    private static String format(TokenCheck.Result tokens, List<String> captureNotes, String decision) {
        String os = System.getProperty("os.name") + " " + System.getProperty("os.arch");
        StringBuilder out = new StringBuilder();
        out.append("# HiDPI theme tokens\n\n");
        out.append("E0-T04. Spike sources live under `spikes/e0-t04/`. Stylesheets use looked-up ");
        out.append("`-fx-ccmm-*` colours; scene graph code has no hex literals.\n\n");
        out.append("- OS: ").append(os).append('\n');
        out.append("- Command: `").append(COMMAND).append("`\n");
        out.append("- Scales: 100%, 150%, 200% (Scale transform on the scene root)\n");
        out.append("- Tree row height: 24 px\n\n");
        out.append("## Contrast (TOKENS.md)\n\n");
        for (String line : tokens.lines()) {
            out.append("- ").append(line).append('\n');
        }
        out.append('\n');
        out.append("## Captures\n\n");
        for (String line : captureNotes) {
            out.append("- ").append(line).append('\n');
        }
        out.append('\n');
        out.append("## Decision\n\n");
        out.append(decision).append('\n');
        if ("go".equals(decision)) {
            out.append('\n');
            out.append("Primary and secondary text match the TOKENS.md ratios. ");
            out.append("Tree rows are not clipped at 100/150/200%. AtlantaFX was not added.\n");
        } else {
            out.append('\n');
            out.append("no-go. Fix the failing token in TOKENS.md; do not add AtlantaFX.\n");
        }
        return out.toString();
    }
}
