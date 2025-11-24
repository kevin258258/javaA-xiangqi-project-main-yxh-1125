package edu.sustech.xiangqi.scene;

import com.almasb.fxgl.app.scene.FXGLMenu;
import com.almasb.fxgl.app.scene.MenuType;
import com.almasb.fxgl.dsl.FXGL;
import edu.sustech.xiangqi.XiangQiApp;
import edu.sustech.xiangqi.model.ChessBoardModel;
import javafx.geometry.Pos;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import java.util.List;

import static com.almasb.fxgl.dsl.FXGL.*;

public class InGameMenuScene extends FXGLMenu {

    private ListView<String> historyListView;

    public InGameMenuScene() {
        super(MenuType.GAME_MENU);

        historyListView = new ListView<>();
        historyListView.setPrefHeight(getAppHeight() - 300);
        historyListView.setPrefWidth(300);

        // --- 2. 【关键修复】强制设置 ListView 的整体样式 ---
        // -fx-control-inner-background: 设置列表内容的背景色为白色
        // -fx-background-color: 设置控件本身的背景色
        historyListView.setStyle(
                "-fx-control-inner-background: white;" +
                        "-fx-background-color: white;" +
                        "-fx-padding: 10;" // 给整个列表加一点内边距
        );

        // --- 3.自定义单元格渲染 (CellFactory) ---
        historyListView.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    // 空行：背景透明或白色
                    setStyle("-fx-background-color: white;");
                } else {
                    setText(item);
                    // 设置字体
                    setFont(FXGL.getAssetLoader().loadFont("HYPixel11pxU-2.ttf").newFont(18));

                    // 【核心】强制文字颜色为黑色，去掉默认的 Padding
                    // -fx-text-fill: black; 强制文字变黑
                    // -fx-background-color: transparent; 让选中时的蓝色背景能显示出来，或者强制设为白色
                    setStyle("-fx-text-fill: brown; -fx-alignment: CENTER-LEFT; -fx-padding: 5;");
                }
            }
        });



        // 1. 创建一个半透明的背景遮罩
        var bg = new Rectangle(getAppWidth(), getAppHeight(), Color.web("000", 0.7));

        // 2. 创建历史记录面板
        var title = new Text("菜单/历史记录");
        title.setFill(Color.WHITE);
        // ... (设置字体)

        historyListView.setPrefHeight(getAppHeight() - 200);



        // 3. 创建其他菜单按钮
        var btnResume = getUIFactoryService().newButton("返回游戏");
        btnResume.setOnAction(e -> fireResume()); // fireResume() 是关闭游戏菜单的内置方法

        var btnExit = getUIFactoryService().newButton("退出到主菜单");
        btnExit.setOnAction(e -> fireExitToMainMenu()); // fireExitToMainMenu() 是内置方法

        // 4. 布局
        var historyBox = new VBox(10, title, historyListView);
        var menuBox = new VBox(15, historyBox, btnResume, btnExit);
        menuBox.setAlignment(Pos.CENTER);

        menuBox.setTranslateX(getAppWidth() / 2.0 - 150); // 居中
        menuBox.setTranslateY(50); // 顶部对齐

        // 5. 添加到场景
        getContentRoot().getChildren().addAll(bg, menuBox);
    }

    @Override
    protected void onUpdate(double tpf) {
        super.onUpdate(tpf);

        XiangQiApp app = (XiangQiApp) FXGL.getApp();

        // 1. 检查 Model 是否已经初始化
        if (app.getModel() != null) {

            // 2. 检查 ListView 当前显示的数据，是否就是 Model 里的那份数据
            // 如果不是（比如刚启动，或者开启了新的一局游戏 Model 换了），就重新绑定
            if (historyListView.getItems() != app.getModel().getMoveHistoryAsObservableList()) {

                historyListView.setItems(app.getModel().getMoveHistoryAsObservableList());
                System.out.println("历史记录已同步！当前步数：" + historyListView.getItems().size());
            }
        }
    }


}