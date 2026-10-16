package dev.bilal.ccmm.app.workbench;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;

/**
 * Five activity buttons that switch the side-bar view.
 * Icon glyphs are temporary letter marks; E1-T08 / ADR-0014 replace them with icons.
 */
public final class ActivityBar extends VBox {
    private final ToggleGroup group = new ToggleGroup();
    private final Map<ActivityView, ToggleButton> buttons = new EnumMap<>(ActivityView.class);
    private Consumer<ActivityView> onSelect = view -> {};
    private ActivityView selected = ActivityView.EXPLORER;

    public ActivityBar() {
        setId("activity-bar");
        setPrefWidth(Workbench.ACTIVITY_WIDTH);
        setMinWidth(Workbench.ACTIVITY_WIDTH);
        setMaxWidth(Workbench.ACTIVITY_WIDTH);
        setAlignment(Pos.TOP_CENTER);
        setSpacing(0);
        for (ActivityView view : ActivityView.values()) {
            ToggleButton button = new ToggleButton(view.glyph());
            button.setId("activity-" + view.name().toLowerCase());
            button.setToggleGroup(group);
            button.setPrefSize(48, 48);
            button.setMinSize(48, 48);
            button.setMaxSize(48, 48);
            button.setTooltip(new Tooltip(view.accessibleName()));
            button.setAccessibleText(view.accessibleName());
            button.setFocusTraversable(true);
            button.setOnAction(event -> select(view));
            buttons.put(view, button);
            getChildren().add(button);
        }
        buttons.get(ActivityView.EXPLORER).setSelected(true);
    }

    public void setOnSelect(Consumer<ActivityView> onSelect) {
        this.onSelect = Objects.requireNonNull(onSelect);
    }

    public ActivityView selected() {
        return selected;
    }

    public ToggleButton button(ActivityView view) {
        return buttons.get(view);
    }

    public void select(ActivityView view) {
        if (view == null) {
            throw new IllegalArgumentException("view is null");
        }
        selected = view;
        ToggleButton button = buttons.get(view);
        if (!button.isSelected()) {
            button.setSelected(true);
        }
        onSelect.accept(view);
    }

    public void setEnabled(boolean enabled) {
        for (ToggleButton button : buttons.values()) {
            button.setDisable(!enabled);
        }
    }
}
