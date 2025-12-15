package edu.sustech.xiangqi.scene;

import com.almasb.fxgl.app.scene.FXGLMenu;
import com.almasb.fxgl.app.scene.MenuType;
import com.almasb.fxgl.dsl.FXGL;
import edu.sustech.xiangqi.XiangQiApp;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
//import javafx.scene.Node;
//import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

public class InGameMenuScene extends FXGLMenu {

    private VBox mainMenuBox;
    private VBox settingsBox;
    private VBox creditsBox;


    public InGameMenuScene() {
        super(MenuType.GAME_MENU);


        // 1. 创建背景遮罩 (半透明黑色)
        Rectangle shadow = new Rectangle(XiangQiApp.APP_WIDTH, XiangQiApp.APP_HEIGHT, Color.color(0, 0, 0, 0.7));
        getContentRoot().getChildren().add(shadow);

        // 2. 初始化各个子菜单面板
        initMainMenu();
        initSettingsMenu();
        initCreditsMenu();

        // 默认显示主菜单面板
        switchMenu(mainMenuBox);
    }

    private void switchMenu(VBox menu) {
        getContentRoot().getChildren().remove(mainMenuBox);
        getContentRoot().getChildren().remove(settingsBox);
        getContentRoot().getChildren().remove(creditsBox);
        getContentRoot().getChildren().add(menu);

        // 居中
        menu.setTranslateX(XiangQiApp.APP_WIDTH / 2.0 - 100);
        menu.setTranslateY(XiangQiApp.APP_HEIGHT / 2.0 - 200);
    }

    //主菜单
    private void initMainMenu() {
        var btnResume = new PixelatedButton("返回游戏", "Button1", () -> fireResume());
        var btnSave = new PixelatedButton("保存游戏", "Button1", () -> {
            XiangQiApp app = (XiangQiApp) FXGL.getApp();
            app.openSaveDialog();
        });

        var btnSettings = new PixelatedButton("设 置", "Button1", () -> switchMenu(settingsBox));

        var btnExit = new PixelatedButton("退出到主菜单", "Button1", () -> fireExitToMainMenu());

        mainMenuBox = new VBox(15, btnResume, btnSave, btnSettings, btnExit);
        mainMenuBox.setAlignment(Pos.CENTER);
    }

    //设置
    private void initSettingsMenu() {
        //音量
        Text volTitle = new Text("音 量");
        volTitle.setFill(Color.WHITE);
        volTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        //显示
        Text volValue = new Text();
        volValue.setFill(Color.LIGHTGREEN);
        volValue.setStyle("-fx-font-size: 18px;");
        // 绑定全局音量
        volValue.textProperty().bind(
                Bindings.format("%.0f%%", FXGL.getSettings().globalSoundVolumeProperty().multiply(100))
        );

        var btnVolDown = new PixelatedButton("-", "Button1", () -> {
            double v = FXGL.getSettings().getGlobalSoundVolume();
            FXGL.getSettings().setGlobalSoundVolume(Math.max(v - 0.1, 0));

            FXGL.getSettings().setGlobalMusicVolume(FXGL.getSettings().getGlobalSoundVolume()); // 同步音乐
        });
        btnVolDown.setMinWidth(60); btnVolDown.setPrefWidth(60);
        btnVolDown.setScaleX(0.3);btnVolDown.setScaleY(0.3);
        btnVolDown.setFontSize(80);
        btnVolDown.setTextY(-20);

        var btnVolUp = new PixelatedButton("+", "Button1", () -> {
            double v = FXGL.getSettings().getGlobalSoundVolume();
            FXGL.getSettings().setGlobalSoundVolume(Math.min(v + 0.1, 1));

            FXGL.getSettings().setGlobalMusicVolume(FXGL.getSettings().getGlobalSoundVolume()); // 同步音乐
        });
        btnVolUp.setMinWidth(60); btnVolUp.setPrefWidth(60);
        btnVolUp.setScaleX(0.3);btnVolUp.setScaleY(0.3);
        btnVolUp.setFontSize(80);
        btnVolUp.setTextY(-20);

        VBox volBox = new VBox(5, volTitle, new javafx.scene.layout.HBox(10, btnVolDown, volValue, btnVolUp));
        ((javafx.scene.layout.HBox)volBox.getChildren().get(1)).setAlignment(Pos.CENTER);
        volBox.setAlignment(Pos.CENTER);


        //屏幕大小
        var btnFullscreen = new PixelatedButton("切换全屏/窗口", "Button1", () -> {
            var stage = FXGL.getPrimaryStage();
            stage.setFullScreen(!stage.isFullScreen());
        });
        btnFullscreen.setFontSize(25);

        //制作人
        var btnCredits = new PixelatedButton("制作人信息", "Button1", () -> switchMenu(creditsBox));

        //返回
        var btnBack = new PixelatedButton("返 回", "Button1", () -> switchMenu(mainMenuBox));

        settingsBox = new VBox(20, volBox, btnFullscreen, btnCredits, btnBack);
        settingsBox.setAlignment(Pos.CENTER);
    }

    //制作人
    private void initCreditsMenu() {
        Text title = new Text("=== 制作团队 ===");
        title.setFill(Color.GOLD);
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");


        String content =
                "主程序: 陶飞翔、尹玺骅\n" +
                        "AI设计: 陶飞翔\n" +
                        "游戏模式: 尹玺骅\n\n" +
                        "特别感谢: 陶伊达老师、王大兴老师\n" +
                        "Sustech CS109.\n" +
                        "2025/12";

        Text text = new Text(content);
        text.setFill(Color.WHITE);
        text.setStyle("-fx-font-size: 18px; -fx-line-spacing: 8px;");
        text.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        var btnBack = new PixelatedButton("返 回", "Button1", () -> switchMenu(settingsBox));

        creditsBox = new VBox(20, title, text, btnBack);
        creditsBox.setAlignment(Pos.CENTER);

        creditsBox.setTranslateY(-50);
    }
}