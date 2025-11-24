package edu.sustech.xiangqi.view;

import com.almasb.fxgl.entity.component.Component;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.Effect;

/**
 控制棋子颜色
 */
public class VisualStateComponent extends Component {

    private ColorAdjust colorAdjust = new ColorAdjust();
    private boolean isActive = true;

    @Override
    public void onAdded() {
        // When this component is added to an entity, apply the effect to its view.
        // We get the view from the entity's ViewComponent.
        entity.getViewComponent().getParent().setEffect(colorAdjust);

        // Initialize with a normal state (not darkened).
        setNormal();
    }

    /**
     * Makes the piece appear darkened (inactive).
     * Brightness is lowered.
     */
    public void setInactive() {
        colorAdjust.setBrightness(-0.5); // Lower brightness, -1.0 is black
        isActive = false;
    }

    /**
     * Makes the piece appear normal (active/selected).
     * Brightness is set back to default.
     */
    public void setNormal() {
        colorAdjust.setBrightness(0.0); // 0.0 is the default, normal brightness
        isActive = true;
    }

    public boolean isActive() {
        return isActive;
    }

    @Override
    public void onRemoved() {
        // Clean up the effect when the component or entity is removed.
        entity.getViewComponent().getParent().setEffect(null);
    }
}