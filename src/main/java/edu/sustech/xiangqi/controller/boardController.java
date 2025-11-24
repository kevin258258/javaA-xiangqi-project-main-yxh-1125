package edu.sustech.xiangqi.controller;

import com.almasb.fxgl.animation.Interpolators;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import edu.sustech.xiangqi.EntityType;
import edu.sustech.xiangqi.XiangQiApp;
import edu.sustech.xiangqi.view.PieceComponent;
import edu.sustech.xiangqi.view.VisualStateComponent;
import edu.sustech.xiangqi.model.*; // Import all piece classes
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.awt.Point;
import java.util.List;
import static com.almasb.fxgl.dsl.FXGL.*;

import static edu.sustech.xiangqi.XiangQiApp.CELL_SIZE;

public class boardController {

    private ChessBoardModel model;
    private Entity selectedEntity = null;

    public boardController(ChessBoardModel model) {
        this.model = model;
    }

    public void onGridClicked(int row, int col) {
        XiangQiApp app = getAppCast();

        // --- 【新增】排局模式处理逻辑 ---
        if (app.isSettingUp()) {
            handleSetupClick(row, col, app);
            return; // 排局模式下不执行后续的正常走棋逻辑
        }

        // --- 以下为正常游戏逻辑 (保持不变) ---
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
     * 【新增】处理排局模式下的点击：放置或移除棋子
     */
    private void handleSetupClick(int row, int col, XiangQiApp app) {
        // 1. 检查该位置是否已有棋子
        AbstractPiece existingPiece = model.getPieceAt(row, col);

        // 2. 如果有点了 UI 面板选中了要放置的棋子类型
        if (app.getSelectedPieceType() != null) {
            // 创建新棋子逻辑对象
            AbstractPiece newPiece = createPiece(app.getSelectedPieceType(), row, col, app.isSelectedPieceRed());

            // 如果是将帅，检查是否已经存在（限制只能有一个）
            if (newPiece instanceof GeneralPiece) {
                if (model.FindKing(newPiece.isRed()) != null) {
                    // 如果原本那个位置就是将，允许覆盖（移动），否则提示
                    // 为了简化，这里采取：先删除已有的将（如果存在），再放置新的
                    AbstractPiece oldKing = model.FindKing(newPiece.isRed());
                    if (oldKing != null && (oldKing.getRow() != row || oldKing.getCol() != col)) {
                        // 提示用户：将帅只能有一个，已移除旧的
                        // 实际逻辑：在 model.addPiece 中并没有自动移除旧的同类，所以这里不管
                        // 但如果用户想放两个将，我们需要禁止，或者移除旧的。
                        // 简单策略：移除旧的 King
                        model.getPieces().remove(oldKing);
                    }
                }
            }

            // 更新模型
            model.addPiece(newPiece);

            // 刷新视图：重新生成所有棋子（简单粗暴但有效）
            app.spawnPiecesFromModel();

        } else {
            // 3. 如果没选中任何工具，且点击了已有棋子 -> 移除它 (橡皮擦功能)
            if (existingPiece != null) {
                model.getPieces().remove(existingPiece);
                app.spawnPiecesFromModel();
            }
        }
    }

    /**
     * 【新增】根据类型字符串创建棋子对象
     */
    private AbstractPiece createPiece(String type, int row, int col, boolean isRed) {
        String name = ""; // 简单起见，这里简化名字，实际可以用 switch 细化
        switch (type) {
            case "General":  name = isRed ? "帅" : "将"; return new GeneralPiece(name, row, col, isRed);
            case "Advisor":  name = isRed ? "仕" : "士"; return new AdvisorPiece(name, row, col, isRed);
            case "Elephant": name = isRed ? "相" : "象"; return new ElephantPiece(name, row, col, isRed);
            case "Horse":    name = "马"; return new HorsePiece(name, row, col, isRed);
            case "Chariot":  name = "车"; return new ChariotPiece(name, row, col, isRed);
            case "Cannon":   name = "炮"; return new CannonPiece(name, row, col, isRed);
            case "Soldier":  name = isRed ? "兵" : "卒"; return new SoldierPiece(name, row, col, isRed);
            default: return new SoldierPiece("兵", row, col, isRed);
        }
    }

    // --- 以下保持原有逻辑不变 ---

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
        Entity entityToMove = this.selectedEntity;
        Point2D startPosition = entityToMove.getPosition();
        Entity capturedEntity = findEntityAt(targetRow, targetCol);

        boolean moveSuccess = model.movePiece(pieceToMove, targetRow, targetCol);

        if (moveSuccess) {
            playMoveAndEndGameAnimation(entityToMove, capturedEntity, startPosition, targetRow, targetCol);
        }
        deselectPiece();
    }

    private void playMoveAndEndGameAnimation(Entity entityToMove, Entity capturedEntity, Point2D startPos, int targetRow, int targetCol) {
        // ... (保持不变) ...
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
            // 调用 App 的刷新方法
            XiangQiApp app = getAppCast();
            app.spawnPiecesFromModel();

            updateTurnIndicator();
            deselectPiece();
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
}