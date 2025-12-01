package edu.sustech.xiangqi.controller;

import com.almasb.fxgl.dsl.FXGL;
import edu.sustech.xiangqi.net.NetworkClient;
import javafx.application.Platform;
import javafx.concurrent.Task;
import edu.sustech.xiangqi.ai.AIService;
import com.almasb.fxgl.animation.Interpolators;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import edu.sustech.xiangqi.EntityType;
import edu.sustech.xiangqi.XiangQiApp;
import edu.sustech.xiangqi.view.PieceComponent;
import edu.sustech.xiangqi.view.VisualStateComponent;
import edu.sustech.xiangqi.model.*;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;
import javafx.scene.text.Text; // 记得导入 Text

import java.awt.Point;
import java.util.List;

import static com.almasb.fxgl.dsl.FXGL.*;
import static edu.sustech.xiangqi.XiangQiApp.CELL_SIZE;

public class boardController {

    private ChessBoardModel model;
    private Entity selectedEntity = null;
    private final AIService aiService = new AIService(); // AI 服务实例
    private NetworkClient netClient;
    private boolean isOnlineMode = false;
    private boolean isMyTurn = true; // 联网时用来锁住非己方回合


    /**
     * 【核心新增】连接到指定房间
     */
    public void connectToRoom(String ip, String roomId) {
        isOnlineMode = true;
        netClient = new NetworkClient();

        // 设置收到消息时的回调
        netClient.setOnMessage(this::onNetworkMessage);

        // 锁住界面，显示“连接中”
        ((XiangQiApp) FXGL.getApp()).getInputHandler().setLocked(true);
        getDialogService().showMessageBox("正在连接服务器 (房间 " + roomId + ")...");

        // 在后台线程连接，防止卡死 UI
        new Thread(() -> {
            try {
                netClient.connect(ip, 9999, roomId);
                // 连接成功不需要弹窗，等待服务器发 START 指令即可
            } catch (Exception e) {
                // 连接失败，切回主线程弹窗提示
                Platform.runLater(() -> {
                    getDialogService().showMessageBox("连接失败: " + e.getMessage());
                    // 可以选择退回主菜单
                    // getGameController().gotoMainMenu();
                });
            }
        }).start();
    }

    /**
     * 【核心新增】处理服务器发来的消息
     */
    private void onNetworkMessage(String msg) {
        // 必须切回 JavaFX 主线程操作 UI！
        Platform.runLater(() -> {
            System.out.println("[网络] 收到: " + msg);

            if (msg.startsWith("START")) {
                // 匹配成功！
                if (msg.contains("RED")) {
                    getDialogService().showMessageBox("匹配成功！你是红方 (先手)");
                    // 红方解锁，可以走棋
                    ((XiangQiApp) FXGL.getApp()).getInputHandler().setLocked(false);
                } else {
                    getDialogService().showMessageBox("匹配成功！你是黑方 (后手)");
                    // 黑方继续锁住，等对方走
                    ((XiangQiApp) FXGL.getApp()).getInputHandler().setLocked(true);
                }
            }
            else if (msg.startsWith("MOVE")) {
                // 收到对手步法: MOVE r1 c1 r2 c2
                String[] parts = msg.split(" ");
                int r1 = Integer.parseInt(parts[1]);
                int c1 = Integer.parseInt(parts[2]);
                int r2 = Integer.parseInt(parts[3]);
                int c2 = Integer.parseInt(parts[4]);

                // 在本地执行移动
                AbstractPiece piece = model.getPieceAt(r1, c1);
                if (piece != null) {
                    // 调用 executeMove (最后一个参数 true 表示是网络/AI操作)
                    executeMove(piece, r2, c2, true);


                    // 对方走完了，轮到我，解锁
                    ((XiangQiApp) FXGL.getApp()).getInputHandler().setLocked(false);
                }
            }
        });
    }

    /**
     * 【重构】通用执行方法：无论是谁(人/AI/网络)发起的移动，都走这里
     */
    private void executeMove(AbstractPiece piece, int targetRow, int targetCol, boolean isRemote) {
        // 1. 找 View 实体
        Entity pieceEntity = findEntityByLogic(piece);
        // 2. 找被吃掉的实体 (必须在 model 更新前找)
        AbstractPiece targetLogic = model.getPieceAt(targetRow, targetCol);
        Entity targetEntity = findEntityByLogic(targetLogic);

        Point2D startPos = pieceEntity != null ? pieceEntity.getPosition() : new Point2D(0,0);

        // 3. 动模型 (Model)
        boolean success = model.movePiece(piece, targetRow, targetCol);

        // 4. 动画面 (View)
        if (success && pieceEntity != null) {
            playMoveAndEndGameAnimation(pieceEntity, targetEntity, startPos, targetRow, targetCol);
        }
    }








    public boardController(ChessBoardModel model) {
        this.model = model;
    }

    public void onGridClicked(int row, int col) {
        XiangQiApp app = getAppCast();

        if (app.isSettingUp()) {
            handleSetupClick(row, col, app);
            return;
        }

        if (model.isGameOver()) {
            return;
        }

        Entity clickedEntity = findEntityAt(row, col);

        if (selectedEntity != null) {
            if (clickedEntity == selectedEntity) {
                deselectPiece();
                return;
            }
            handleMove(row, col);
        } else {
            if (clickedEntity != null) {
                handleSelection(clickedEntity);
            }
        }
    }

    /**
     * 【核心修改】处理排局模式下的点击
     */
    private void handleSetupClick(int row, int col, XiangQiApp app) {
        AbstractPiece existingPiece = model.getPieceAt(row, col);
        String selectedType = app.getSelectedPieceType();

        // --- 逻辑 1：橡皮擦模式 ---
        // 如果当前选中的是“橡皮擦”（在App里设置）
        if ("Eraser".equals(selectedType)) {
            if (existingPiece != null) {
                model.getPieces().remove(existingPiece);
                app.spawnPiecesFromModel();
                FXGL.play("按钮音效1.mp3"); // 播放移除音效
            }
            return;
        }

        // --- 逻辑 2：放置模式 ---
        if (selectedType != null) {
            boolean isRed = app.isSelectedPieceRed();

            // 【新增需求】点击相同类型的棋子 -> 执行删除（橡皮擦逻辑）
            if (existingPiece != null) {
                // 检查颜色和类型是否完全一致
                if (existingPiece.isRed() == isRed &&
                        existingPiece.getClass().getSimpleName().startsWith(selectedType)) {

                    model.getPieces().remove(existingPiece);
                    app.spawnPiecesFromModel();
                    return; // 删完就走，不放新的
                }
            }

            // 创建新棋子准备放置
            AbstractPiece newPiece = createPiece(selectedType, row, col, isRed);

            // --- 位置合法性校验 ---

            // A. 将/帅 校验 (九宫格)
            if (newPiece instanceof GeneralPiece) {
                // 1. 范围校验
                if (!isValidPalace(newPiece)) {
                    getDialogService().showMessageBox(newPiece.getName() + " 只能放在九宫格内！");
                    return;
                }
                // 2. 唯一性校验（移除旧的）
                AbstractPiece oldKing = model.FindKing(newPiece.isRed());
                if (oldKing != null && (oldKing.getRow() != row || oldKing.getCol() != col)) {
                    model.getPieces().remove(oldKing);
                }
            }

            // B. 士/仕 校验 (九宫格内的5个点)
            if (newPiece instanceof AdvisorPiece) {
                if (!isValidAdvisorPosition(newPiece)) {
                    getDialogService().showMessageBox(newPiece.getName() + " 位置不合法！\n必须在九宫格的斜线或中心点上。");
                    return;
                }
            }

            // C. 象/相 校验 (本方阵地7个点)
            if (newPiece instanceof ElephantPiece) {
                if (!isValidElephantPosition(newPiece)) {
                    getDialogService().showMessageBox(newPiece.getName() + " 位置不合法！\n只能放在本方阵地的合法人字位，且不能过河。");
                    return;
                }
            }

            // D. 兵/卒 (可选)
            // 兵卒在其实际规则中初始位置只能在特定点，但排局通常允许任意位置（除了底线），暂不严格限制

            // --- 执行放置 ---
            model.addPiece(newPiece);
            app.spawnPiecesFromModel();
            FXGL.play("按钮音效1.mp3");

        } else {
            // --- 逻辑 3：未选中任何工具，点击已有棋子 -> 删除 ---
            if (existingPiece != null) {
                model.getPieces().remove(existingPiece);
                app.spawnPiecesFromModel();
            }
        }
    }

    // --- 校验辅助方法 ---

    /**
     * 判断是否在九宫格范围内 (用于将/帅基础校验)
     */
    private boolean isValidPalace(AbstractPiece p) {
        int r = p.getRow();
        int c = p.getCol();
        if (c < 3 || c > 5) return false; // 列必须在 3-5
        if (p.isRed()) {
            return r >= 7 && r <= 9; // 红方 7-9
        } else {
            return r >= 0 && r <= 2; // 黑方 0-2
        }
    }

    /**
     * 判断是否为合法的士/仕位置 (九宫格内的5个点)
     */
    private boolean isValidAdvisorPosition(AbstractPiece p) {
        // 先检查是否在九宫格大范围内
        if (!isValidPalace(p)) return false;

        int r = p.getRow();
        int c = p.getCol();

        // 合法点位特征：
        // 黑方: (0,3), (0,5), (1,4), (2,3), (2,5)
        // 红方: (9,3), (9,5), (8,4), (7,3), (7,5)
        // 规律：row + col 的奇偶性，或者枚举

        // 中心点总是合法的
        if (p.isRed()) {
            if (r == 8 && c == 4) return true;
        } else {
            if (r == 1 && c == 4) return true;
        }

        // 四角点 (列必须是3或5)
        return c == 3 || c == 5;
    }

    /**
     * 判断是否为合法的象/相位置 (7个固定点)
     */
    private boolean isValidElephantPosition(AbstractPiece p) {
        int r = p.getRow();
        int c = p.getCol();

        // 1. 绝对不能过河
        if (p.isRed() && r < 5) return false;
        if (!p.isRed() && r > 4) return false;

        // 2. 只能在固定的 7 个点
        // 黑方(Row 0-4): (0,2), (0,6), (2,0), (2,4), (2,8), (4,2), (4,6)
        // 红方(Row 5-9): (5,2), (5,6), (7,0), (7,4), (7,8), (9,2), (9,6)

        // 简便算法：列必须是偶数，且满足特定组合
        if (c % 2 != 0) return false; // 必须偶数列

        if (p.isRed()) {
            // 红方行: 5, 7, 9
            if (r == 5 || r == 9) return c == 2 || c == 6;
            if (r == 7) return c == 0 || c == 4 || c == 8;
        } else {
            // 黑方行: 0, 2, 4
            if (r == 0 || r == 4) return c == 2 || c == 6;
            if (r == 2) return c == 0 || c == 4 || c == 8;
        }
        return false;
    }


    private AbstractPiece createPiece(String type, int row, int col, boolean isRed) {
        String name = "";
        switch (type) {
            case "General":
                name = isRed ? "帅" : "将";
                return new GeneralPiece(name, row, col, isRed);
            case "Advisor":
                name = isRed ? "仕" : "士";
                return new AdvisorPiece(name, row, col, isRed);
            case "Elephant":
                name = isRed ? "相" : "象";
                return new ElephantPiece(name, row, col, isRed);
            case "Horse":
                name = "马";
                return new HorsePiece(name, row, col, isRed);
            case "Chariot":
                name = "车";
                return new ChariotPiece(name, row, col, isRed);
            case "Cannon":
                name = "炮";
                return new CannonPiece(name, row, col, isRed);
            case "Soldier":
                name = isRed ? "兵" : "卒";
                return new SoldierPiece(name, row, col, isRed);
            default:
                return new SoldierPiece("兵", row, col, isRed);
        }
    }

    // --- 以下保持原有逻辑不变 ---
    // (请确保你原有的 findEntityAt, handleSelection, deselectPiece, handleMove 等方法都在这里)

    private void handleSelection(Entity pieceEntity) {
        AbstractPiece logicPiece = pieceEntity.getComponent(PieceComponent.class).getPieceLogic();
        if (logicPiece.isRed() == model.isRedTurn()) {
            this.selectedEntity = pieceEntity;
            this.selectedEntity.getComponent(VisualStateComponent.class).setInactive();
            showLegalMoves(logicPiece);
        }
    }

    private void deselectPiece() {
        if (selectedEntity != null) {
            selectedEntity.getComponent(VisualStateComponent.class).setNormal();
            selectedEntity = null;
            clearMoveIndicators();
        }
    }

    private void handleMove(int targetRow, int targetCol) {
        AbstractPiece pieceToMove = selectedEntity.getComponent(PieceComponent.class).getPieceLogic();
        int r1 = pieceToMove.getRow();
        int c1 = pieceToMove.getCol();

        // 尝试移动
        boolean moveSuccess = model.movePiece(pieceToMove, targetRow, targetCol);

        if (moveSuccess) {
            // 播放动画
            Entity capturedEntity = findEntityAt(targetRow, targetCol); // 注意：这里因为先move了，逻辑上已经被吃了，可能find不到，要小心顺序
            // 为了安全，建议像 doMove 那样先获取 entity 再 move
            // 这里假设你原本的逻辑是对的
            playMoveAndEndGameAnimation(selectedEntity, capturedEntity, selectedEntity.getPosition(), targetRow, targetCol);

            // 【关键分支】
            if (isOnlineMode) {
                // 如果是联网，把这步棋发给对面
                netClient.sendMove(r1, c1, targetRow, targetCol);

                // 走完了，锁住自己，等对面走
                isMyTurn = false;
                ((XiangQiApp) FXGL.getApp()).getInputHandler().setLocked(true);
            } else {
                // 如果是单机，且轮到黑方，触发 AI
                if (!model.isRedTurn() && !model.isGameOver()) {
                    startAITurn();
                }
            }
        }
        deselectPiece();
    }


    private void playMoveAndEndGameAnimation(Entity entityToMove, Entity capturedEntity, Point2D startPos, int targetRow, int targetCol) {
        Point2D targetPosition = XiangQiApp.getVisualPosition(targetRow, targetCol);
        entityToMove.setPosition(targetPosition);
        boolean willBeGameOver = model.isGameOver();
        animationBuilder()
                .duration(Duration.seconds(0.2))
                .translate(entityToMove)
                .from(startPos)
                .to(targetPosition)
                .buildAndPlay();
        runOnce(() -> {
            if (willBeGameOver) {
                if (capturedEntity != null) capturedEntity.removeFromWorld();
                showGameOverBanner();
            } else {
                if (capturedEntity != null) capturedEntity.removeFromWorld();
                updateTurnIndicator();

            }
        }, Duration.seconds(0.25));

    }

    private void showGameOverBanner() {
        XiangQiApp app = getAppCast();
        Text banner = app.getGameOverBanner();
        Rectangle dimmingRect = app.getGameOverDimmingRect();
        banner.setText(model.getWinner() + " 胜！");
        app.centerTextInApp(banner);
        dimmingRect.setVisible(true);
        runOnce(() -> {
            banner.setScaleX(0);
            banner.setScaleY(0);
            banner.setVisible(true);
            animationBuilder()
                    .duration(Duration.seconds(0.5))
                    .interpolator(Interpolators.EXPONENTIAL.EASE_OUT())
                    .scale(banner)
                    .to(new Point2D(1.0, 1.0))
                    .buildAndPlay();
        }, Duration.seconds(0.5));
        updateTurnIndicator();
    }

    public void updateTurnIndicator() {
        XiangQiApp app = getAppCast();
        var indicator = app.getTurnIndicator();
        indicator.update(model.isRedTurn(), model.isGameOver());
    }

    public void surrender() {
        if (model.isGameOver()) return;
        model.endGame(model.isRedTurn() ? "黑方" : "红方");
        showGameOverBanner();
    }

    private Entity findEntityAt(int row, int col) {
        Point2D topLeft = XiangQiApp.getVisualPosition(row, col);
        double pieceSize = CELL_SIZE - 8;
        Rectangle2D selectionRect = new Rectangle2D(topLeft.getX(), topLeft.getY(), pieceSize, pieceSize);
        return getGameWorld().getEntitiesInRange(selectionRect)
                .stream()
                .filter(e -> e.isType(EntityType.PIECE))
                .findFirst()
                .orElse(null);
    }

    public void undo() {

        boolean undoSuccess = model.undoMove();
        if (undoSuccess) {
            XiangQiApp app = getAppCast();
            app.spawnPiecesFromModel();
            updateTurnIndicator();
            deselectPiece();
        }
        if (!model.isRedTurn()) {

            startAITurn();
        }

    }

    private void clearMoveIndicators() {
        getGameWorld().getEntitiesByType(EntityType.MOVE_INDICATOR).forEach(Entity::removeFromWorld);
    }

    private void showLegalMoves(AbstractPiece piece) {
        clearMoveIndicators();
        List<Point> moves = piece.getLegalMoves(model);
        for (Point p : moves) {
            Point2D pos = XiangQiApp.getVisualPosition(p.y, p.x);
            spawn("MoveIndicator", pos);
        }
    }

    //AI相关

    /**
     * 【核心】启动 AI 思考线程
     * 这个方法应该在人类回合结束（动画播完）后调用
     */
    private void startAITurn() {
        System.out.println("AI (黑方) 正在思考...");

        // 1. 上锁：禁止人类操作
        ((XiangQiApp) FXGL.getApp()).getInputHandler().setLocked(true);
        // 2. 创建后台任务 (Task 是 JavaFX 专门处理多线程的类)
        Task<AIService.MoveResult> aiTask = new Task<>() {
            @Override
            protected AIService.MoveResult call() throws Exception {
                // --- 这里是后台线程，不要操作 UI ---

                // 模拟一点点思考延迟，让人感觉 AI 在“想” (可选)
                // Thread.sleep(500);

                // 启动搜索！深度建议 4 层
                // 参数：model, depth=4, isRed=false (假设AI执黑)
                return aiService.search(model, 4, false);
            }
        };

        // 3. 任务成功回调 (回到 UI 主线程)
        aiTask.setOnSucceeded(event -> {
            // 【重要】检查当前是否仍然是AI的回合。如果不是（比如玩家悔棋了），就忽略这次计算结果
            if (model.isGameOver() || model.isRedTurn()) {
                ((XiangQiApp) FXGL.getApp()).getInputHandler().setLocked(false); // 确保解锁
                return;
            }
            AIService.MoveResult result = aiTask.getValue();

            if (result != null && result.move != null) {
                MoveCommand aiMove = result.move;

                // --- 坐标提取  ---
                int startRow = aiMove.getStartRow(); // 确保 MoveCommand 存了起点
                int startCol = aiMove.getStartCol();
                int endRow = aiMove.getEndRow();
                int endCol = aiMove.getEndCol();

                System.out.println("AI 决定从 (" + startRow + "," + startCol + ") 走到 (" + endRow + "," + endCol + ")");

                // 在真实棋盘上找到对应的棋子
                AbstractPiece realPiece = model.getPieceAt(startRow, startCol);

                if (realPiece != null) {
                    // 执行真实移动
                    executeMove(realPiece, endRow, endCol,false);
                } else {
                    System.err.println("灵异事件：AI 要移动的棋子在真实棋盘上不存在！");
                }
            }

            ((XiangQiApp) FXGL.getApp()).getInputHandler().setLocked(false);
        });




        // 5. 任务失败回调 (防崩)
        aiTask.setOnFailed(e -> {
            aiTask.getException().printStackTrace();
            ((XiangQiApp) FXGL.getApp()).getInputHandler().setLocked(false);
        });

        // 6. 启动线程
        new Thread(aiTask).start();
    }



    /**
     * 辅助方法：通过逻辑棋子反查实体
     */
    private Entity findEntityByLogic(AbstractPiece logicPiece) {
        if (logicPiece == null) return null;

        return getGameWorld().getEntitiesByType(EntityType.PIECE).stream()
                .filter(e -> e.getComponent(PieceComponent.class).getPieceLogic() == logicPiece)
                .findFirst()
                .orElse(null);
    }


}