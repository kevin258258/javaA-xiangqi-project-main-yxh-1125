package edu.sustech.xiangqi.scene;

import com.almasb.fxgl.dsl.FXGL;
import javafx.beans.binding.Bindings;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.Effect;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class PixelatedButton extends Pane {

    private ImageView background;
    private Text text;
    private Runnable action;

    public PixelatedButton(String label,String imageName, Runnable action) {
        this.action = action;

        // 1. 加载按钮背景图片
        background = new ImageView(FXGL.getAssetLoader().loadTexture(  imageName + ".png").getImage());

        // 2. 创建按钮上的文字
        text = new Text(label);
        text.setFont(FXGL.getAssetLoader().loadFont("HYPixel11pxU-2.ttf").newFont(40)); // 使用你的菜单字体
        text.setFill(Color.WHITE);

        // 3. 将图片和文字都添加到 Pane 中
        getChildren().addAll(background, text);

        // 4. 让文字在按钮图片上居中
        // 这段绑定代码会自动保持文字居中，即使窗口大小或字体改变
        text.translateXProperty().bind(
                background.fitWidthProperty().subtract(text.layoutBoundsProperty().get().getWidth()).divide(2)
        );
        text.translateYProperty().bind(
                background.fitHeightProperty().subtract(text.layoutBoundsProperty().get().getHeight()).divide(2).add(text.getFont().getSize() * 0.7)
        );

        // --- 5. 添加交互反馈 ---

        // 鼠标进入时，播放音效
        setOnMouseEntered(e -> {
            FXGL.play("按钮音效1.mp3");
        });

        // 鼠标按下时，给按钮一个“按下”的效果
        setOnMousePressed(e -> {
            // 方案A: 如果你有 button_down.png
            background.setImage(FXGL.getAssetLoader().loadTexture("Press.png").getImage());
            FXGL.play("按钮音效1.mp3");

        });

        // 鼠标释放时，执行动作并恢复正常状态
        setOnMouseReleased(e -> {
            // 恢复正常外观
            // 方案A:
            background.setImage(FXGL.getAssetLoader().loadTexture("Button1.png").getImage());


            // 执行绑定的功能
            if (this.action != null) {
                this.action.run();
            }
        });
    }
}