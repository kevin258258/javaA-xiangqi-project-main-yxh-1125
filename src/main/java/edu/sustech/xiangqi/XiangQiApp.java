package edu.sustech.xiangqi;

import com.almasb.fxgl.dsl.FXGL;
import edu.sustech.xiangqi.scene.*;
import com.almasb.fxgl.app.scene.FXGLMenu;
import com.almasb.fxgl.app.scene.SceneFactory;
import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.entity.SpawnData;
import edu.sustech.xiangqi.model.*;
import edu.sustech.xiangqi.view.XiangQiFactory;
import edu.sustech.xiangqi.controller.InputHandler;
import edu.sustech.xiangqi.controller.boardController;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import com.almasb.fxgl.input.Input;
import com.almasb.fxgl.input.UserAction;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import static com.almasb.fxgl.dsl.FXGL.*;

public class XiangQiApp extends GameApplication {

    // --- 常量定义 ---
    public static final int CELL_SIZE = 90;
    public static final int MARGIN = 31;
    public static final int HORIZONTAL_PADDING_MIN = 70;
    public static final int UI_GAP = 50;
    public static final int UI_WIDTH = 160;

    public static final int BOARD_WIDTH = 796;
    public static final int BOARD_HEIGHT = 887;

    public static final int APP_WIDTH = UI_WIDTH + UI_GAP + BOARD_WIDTH + UI_GAP + UI_WIDTH;
    public static final int APP_HEIGHT = BOARD_HEIGHT + 50;

    // 棋盘在画面中居中的起始 X 坐标
    public static final double BOARD_START_X = (APP_WIDTH - BOARD_WIDTH) / 2.0;

    // 文本提示
    private Text gameOverBanner;
    private Rectangle gameOverDimmingRect;
    private TurnIndicator turnIndicator;
    private Font gameFont;

    // --- 状态变量 ---
    private boolean isCustomMode = false;
    private boolean isSettingUp = false;
    private String selectedPieceType = null;
    private boolean selectedPieceIsRed = true;

    // UI 容器引用
    private VBox leftSetupPanel;
    private VBox rightSetupPanel;
    private VBox standardGameUI;
    // 【新增】左下角先手选择面板引用
    private VBox turnSelectionPanel;

    private ChessBoardModel model;
    private boardController boardController;
    private InputHandler inputHandler;

    // --- Getters & Setters ---
    public Text getGameOverBanner() { return gameOverBanner; }
    public Rectangle getGameOverDimmingRect() { return gameOverDimmingRect; }
    public void setCustomMode(boolean customMode) { this.isCustomMode = customMode; }
    public boolean isSettingUp() { return isSettingUp; }
    public String getSelectedPieceType() { return selectedPieceType; }
    public boolean isSelectedPieceRed() { return selectedPieceIsRed; }
    public ChessBoardModel getModel() { return model; }
    public TurnIndicator getTurnIndicator() { return turnIndicator; }

    public void centerTextInApp(Text text) {
        double textWidth = text.getLayoutBounds().getWidth();
        double textHeight = text.getLayoutBounds().getHeight();
        double centerX = (APP_WIDTH - textWidth) / 2;
        double centerY = (APP_HEIGHT - textHeight) / 2 + text.getFont().getSize() * 0.3;
        text.setTranslateX(centerX);
        text.setTranslateY(centerY);
    }

    public static Point2D getVisualPosition(int row, int col) {
        double centerX = BOARD_START_X + MARGIN + col * CELL_SIZE;
        double centerY = MARGIN + row * CELL_SIZE;
        double pieceRadius = (CELL_SIZE - 8) / 2.0;
        double topLeftX = centerX - pieceRadius;
        double topLeftY = centerY - pieceRadius;
        return new Point2D(topLeftX, topLeftY);
    }

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setTitle("中国象棋 1.0");
        settings.setVersion("1.0");
        settings.setWidth(APP_WIDTH);
        settings.setHeight(APP_HEIGHT);
        settings.setMainMenuEnabled(true);
        settings.setSceneFactory(new SceneFactory() {
            @Override
            public FXGLMenu newMainMenu() { return new MainMenuScene(); }
            @Override
            public FXGLMenu newGameMenu() { return new InGameMenuScene(); }
        });
    }

    @Override
    protected void onPreInit() {
        try {
            gameFont = getAssetLoader().loadFont("HYPixel11pxU-2.ttf").newFont(20);
        } catch (Exception e) {
            System.out.println("字体加载失败，使用默认字体");
            gameFont = Font.font("System", FontWeight.BOLD, 20);
        }
    }

    @Override
    protected void initGame() {
        getGameWorld().addEntityFactory(new XiangQiFactory());
        this.model = new ChessBoardModel();

        spawn("background", 0, 0);
        spawn("board", BOARD_START_X, 0);

        this.boardController = new boardController(this.model);
        this.inputHandler = new InputHandler(this.boardController);

        if (isCustomMode) {
            model.clearBoard();
            isSettingUp = true;
            selectedPieceType = null;
        } else {
            isSettingUp = false;
            spawnPiecesFromModel();
        }
    }

    public void spawnPiecesFromModel() {
        getGameWorld().getEntitiesByType(EntityType.PIECE).forEach(entity -> entity.removeFromWorld());
        for (AbstractPiece pieceLogic : model.getPieces()) {
            String colorPrefix = pieceLogic.isRed() ? "Red" : "Black";
            String pieceTypeName = pieceLogic.getClass().getSimpleName().replace("Piece", "");
            String entityID = colorPrefix + pieceTypeName;
            Point2D visualPos = getVisualPosition(pieceLogic.getRow(), pieceLogic.getCol());
            spawn(entityID, new SpawnData(visualPos).put("pieceLogic", pieceLogic));
        }
    }

    @Override
    protected void initUI() {
        gameOverDimmingRect = new Rectangle(APP_WIDTH, APP_HEIGHT, Color.web("000", 0.0));
        gameOverDimmingRect.setVisible(false);
        gameOverDimmingRect.setMouseTransparent(true);

        gameOverBanner = new Text();
        try {
            gameOverBanner.setFont(getAssetLoader().loadFont("HYPixel11pxU-2.ttf").newFont(80));
        } catch (Exception e) {
            gameOverBanner.setFont(Font.font(80));
        }
        gameOverBanner.setFill(Color.BROWN);
        gameOverBanner.setStroke(Color.BLACK);
        gameOverBanner.setStrokeWidth(3);
        gameOverBanner.setEffect(new DropShadow(15, Color.BLACK));
        gameOverBanner.setVisible(false);
        addUINode(gameOverDimmingRect);
        addUINode(gameOverBanner);

        if (isCustomMode) {
            initSetupUI();
        } else {
            initStandardGameUI();
        }
    }

    private void initStandardGameUI() {
        double uiX = BOARD_START_X + BOARD_WIDTH + UI_GAP;

        var btnUndo = new PixelatedButton("悔棋", "Button1", () -> { if (boardController != null) boardController.undo(); });
        var btnSurrender = new PixelatedButton("投降", "Button1", () -> { if (boardController != null) boardController.surrender(); });
        var btnAIHint = new PixelatedButton("AI提示", "Button1", () -> System.out.println("AI提示..."));
        var btnHistory = new PixelatedButton("历史记录", "Button1", () -> getGameController().gotoGameMenu());

        standardGameUI = new VBox(10, btnUndo, btnSurrender, btnAIHint, btnHistory);
        standardGameUI.setPrefWidth(UI_WIDTH);

        addUINode(standardGameUI, uiX, 50);

        turnIndicator = new TurnIndicator();
        turnIndicator.update(model.isRedTurn(), false);
        addUINode(turnIndicator, uiX, 750);
    }

    private void initSetupUI() {
        // --- 1. 左侧棋子库 ---
        double leftPanelX = BOARD_START_X - UI_GAP - UI_WIDTH;
        // 保证不贴边，至少留 20px
        double safeLeftX = Math.max(20, leftPanelX);

        leftSetupPanel = createPiecePalette(true);
        leftSetupPanel.setTranslateX(safeLeftX);
        leftSetupPanel.setTranslateY(50);

        // --- 2. 右侧棋子库 + 开始按钮 ---
        double rightPanelX = BOARD_START_X + BOARD_WIDTH + UI_GAP;
        rightSetupPanel = createPiecePalette(false);
        rightSetupPanel.setTranslateX(rightPanelX);
        rightSetupPanel.setTranslateY(50);

        // --- 3. 【修改】左下角：先手选择按钮 ---
        ToggleGroup turnGroup = new ToggleGroup();
        ToggleButton rbRedFirst = createStyledToggleButton("红先", true);
        ToggleButton rbBlackFirst = createStyledToggleButton("黑先", false);
        rbRedFirst.setToggleGroup(turnGroup);
        rbBlackFirst.setToggleGroup(turnGroup);
        rbRedFirst.setSelected(true);

        // 创建一个单独的面板放置它们
        Label turnLabel = new Label("先手选择");
        turnLabel.setFont(gameFont);
        turnLabel.setTextFill(Color.WHITE);

        turnSelectionPanel = new VBox(10, turnLabel, rbRedFirst, rbBlackFirst);
        turnSelectionPanel.setAlignment(Pos.CENTER_LEFT);

        // 设置位置：横坐标与左侧棋子库对齐，纵坐标在屏幕底部偏上一点
        turnSelectionPanel.setTranslateX(safeLeftX);
        turnSelectionPanel.setTranslateY(APP_HEIGHT - 200); // 距离底部 200px，根据需要调整

        // --- 4. 右侧：开始按钮和提示 ---
        Button btnStartCustom = new Button("开始对局");
        btnStartCustom.setFont(gameFont);
        btnStartCustom.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 5;");
        btnStartCustom.setOnAction(e -> {
            boolean isRedFirst = rbRedFirst.isSelected();
            tryStartCustomGame(isRedFirst);
        });

        Label hintLabel = new Label("操作提示:\n1.点击两侧棋子选中\n2.点击棋盘放置\n3.点击已放棋子移除");
        hintLabel.setFont(getAssetLoader().loadFont("HYPixel11pxU-2.ttf").newFont(16));
        hintLabel.setStyle("-fx-text-fill: white; -fx-padding: 10; -fx-background-color: rgba(0,0,0,0.5); -fx-background-radius: 5;");
        hintLabel.setWrapText(true);
        hintLabel.setPrefWidth(UI_WIDTH);

        VBox controlBox = new VBox(20, hintLabel, btnStartCustom);
        controlBox.setAlignment(Pos.CENTER);
        controlBox.setStyle("-fx-padding: 30 0 0 0;");
        rightSetupPanel.getChildren().add(controlBox);

        addUINode(leftSetupPanel);
        addUINode(rightSetupPanel);
        // 添加新的左下角面板
        addUINode(turnSelectionPanel);
    }

    private ToggleButton createStyledToggleButton(String text, boolean isRed) {
        ToggleButton tb = new ToggleButton(text);
        tb.setFont(gameFont);
        tb.setPrefWidth(UI_WIDTH * 0.8);
        String baseColor = isRed ? "#ffcccc" : "#cccccc";
        String selectedColor = isRed ? "#ff9999" : "#999999";
        tb.setStyle("-fx-base: " + baseColor + "; -fx-background-radius: 5;");
        tb.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                tb.setStyle("-fx-base: " + selectedColor + "; -fx-background-radius: 5; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 5, 0, 0, 1);");
            } else {
                tb.setStyle("-fx-base: " + baseColor + "; -fx-background-radius: 5;");
            }
        });
        return tb;
    }

    private VBox createPiecePalette(boolean isRed) {
        VBox box = new VBox(10);
        box.setAlignment(Pos.TOP_CENTER);
        box.setPrefWidth(UI_WIDTH);

        String colorName = isRed ? "红方" : "黑方";
        Label title = new Label(colorName + "棋库");
        title.setFont(gameFont);
        title.setTextFill(isRed ? Color.web("#ff3333") : Color.web("#333333"));
        title.setStyle("-fx-padding: 5 15; -fx-background-color: rgba(255,255,255,0.7); -fx-background-radius: 10;");
        box.getChildren().add(title);

        String[] types = {"General", "Advisor", "Elephant", "Horse", "Chariot", "Cannon", "Soldier"};

        for (String type : types) {
            String colorPrefix = isRed ? "Red" : "Black";
            String textureName = colorPrefix + type + ".png";
            ImageView pieceImage;
            try {
                pieceImage = new ImageView(getAssetLoader().loadTexture(textureName).getImage());
                pieceImage.setFitWidth(55);
                pieceImage.setFitHeight(55);
                pieceImage.setPreserveRatio(true);
            } catch (Exception e) {
                System.err.println("无法加载棋子图片用于UI: " + textureName);
                pieceImage = null;
            }

            Button btn = new Button();
            btn.setPrefWidth(70);
            btn.setPrefHeight(70);

            if (pieceImage != null) {
                btn.setGraphic(pieceImage);
                btn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-border-width: 2; -fx-border-radius: 5;");
                btn.setOnAction(e -> {
                    leftSetupPanel.getChildren().stream().filter(n -> n instanceof Button).forEach(n -> n.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;"));
                    rightSetupPanel.getChildren().stream().filter(n -> n instanceof Button).forEach(n -> n.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;"));
                    btn.setStyle("-fx-background-color: rgba(255,255,0,0.3); -fx-border-color: yellow; -fx-border-width: 2; -fx-border-radius: 5;");
                    this.selectedPieceType = type;
                    this.selectedPieceIsRed = isRed;
                    FXGL.play("按钮音效1.mp3");
                });
            } else {
                btn.setText(type);
            }
            box.getChildren().add(btn);
        }
        return box;
    }

    private void tryStartCustomGame(boolean isRedFirst) {
        if (model.FindKing(true) == null || model.FindKing(false) == null) {
            getDialogService().showMessageBox("规则错误：\n红黑双方必须各有一只帅/将才能开始！");
            return;
        }
        removeUINode(leftSetupPanel);
        removeUINode(rightSetupPanel);
        // 【新增】同时移除左下角的先手选择面板
        removeUINode(turnSelectionPanel);

        model.setRedTurn(isRedFirst);
        this.isSettingUp = false;
        this.selectedPieceType = null;

        initStandardGameUI();

        turnIndicator.update(isRedFirst, false);
        getDialogService().showMessageBox("排局开始！\n由 " + (isRedFirst ? "红方" : "黑方") + " 先行。");
    }

    @Override
    protected void initInput() {
        Input input = getInput();
        UserAction clickAction = new UserAction("Click") {
            @Override
            protected void onActionEnd() {
                if (inputHandler != null) {
                    inputHandler.handleMouseClick(input.getMousePositionWorld());
                }
            }
        };
        input.addAction(clickAction, MouseButton.PRIMARY);
    }

    public static void main(String[] args) { launch(args); }
}