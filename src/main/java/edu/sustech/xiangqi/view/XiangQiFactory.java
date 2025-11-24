package edu.sustech.xiangqi.view;

import com.almasb.fxgl.texture.Texture;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.almasb.fxgl.texture.Texture;
import edu.sustech.xiangqi.EntityType;
import edu.sustech.xiangqi.XiangQiApp;
import edu.sustech.xiangqi.view.PieceComponent;
import edu.sustech.xiangqi.view.VisualStateComponent;
import edu.sustech.xiangqi.model.*;
import javafx.scene.effect.Glow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import static com.almasb.fxgl.dsl.FXGL.entityBuilder;
import static edu.sustech.xiangqi.XiangQiApp.*;

public class XiangQiFactory implements EntityFactory {

    // --- Spawn 方法保持不变 ---
    // (这里需要为你所有的棋子类型都添加上)
    @Spawns("RedChariot") public Entity newRedChariot(SpawnData data) { return newPiece(data); }
    @Spawns("BlackChariot") public Entity newBlackChariot(SpawnData data) { return newPiece(data); }
    @Spawns("RedHorse") public Entity newRedHorse(SpawnData data) { return newPiece(data); }
    @Spawns("BlackHorse") public Entity newBlackHorse(SpawnData data) { return newPiece(data); }
    @Spawns("RedElephant") public Entity newRedElephant(SpawnData data) { return newPiece(data); }
    @Spawns("BlackElephant") public Entity newBlackElephant(SpawnData data) { return newPiece(data); }
    @Spawns("RedAdvisor") public Entity newRedAdvisor(SpawnData data) { return newPiece(data); }
    @Spawns("BlackAdvisor") public Entity newBlackAdvisor(SpawnData data) { return newPiece(data); }
    @Spawns("RedGeneral") public Entity newRedGeneral(SpawnData data) { return newPiece(data); }
    @Spawns("BlackGeneral") public Entity newBlackGeneral(SpawnData data) { return newPiece(data); }
    @Spawns("RedCannon") public Entity newRedCannon(SpawnData data) { return newPiece(data); }
    @Spawns("BlackCannon") public Entity newBlackCannon(SpawnData data) { return newPiece(data); }
    @Spawns("RedSoldier") public Entity newRedSoldier(SpawnData data) { return newPiece(data); }
    @Spawns("BlackSoldier") public Entity newBlackSoldier(SpawnData data) { return newPiece(data); }


    @Spawns("board")
    public Entity newBoard(SpawnData data) {
        // 我们不再缩放棋盘，而是直接加载它
        // 这样可以保证棋盘网格的线条清晰，不模糊
        return entityBuilder(data)
                .type(EntityType.BOARD)
                .view("ChessBoard.png")
                .zIndex(-1)
                .build();
    }

    @Spawns("MoveIndicator")
    public Entity newMoveIndicator(SpawnData data) {
        // 稍微比格子小一点的半透明绿色圆点
        Texture image = FXGL.getAssetLoader().loadTexture("选中.png");
        int padding = 8;
        image.setFitWidth(CELL_SIZE - padding);
        image.setFitHeight(CELL_SIZE - padding);

        // (可选但强烈推荐) 保持图片原始的长宽比，防止棋子被压扁或拉长

        return entityBuilder(data)
                .type(EntityType.MOVE_INDICATOR)
                // 居中显示
                .viewWithBBox(image)
                .zIndex(100) // 保证显示在棋子上方
                .build();
    }

    @Spawns("background")
    public Entity newBackground(SpawnData data) {
        // 1. 加载背景图片
        Texture bgView = FXGL.getAssetLoader().loadTexture("背景.jpg");



        // 2. 强制将其尺寸拉伸到和整个窗口一样大
        bgView.setFitWidth(APP_WIDTH);
        bgView.setFitHeight(APP_HEIGHT);

        return entityBuilder(data)
                .type(EntityType.BACKGROUND)
                .view(bgView)
                // 3. 【最关键的一步】设置一个比棋盘更低的 zIndex
                // 棋盘是 -1，所以背景必须是 -2 或更低
                .zIndex(-2)
                .build();
    }

    /**
     * 【最终修正版 - 解决棋子大小不一的问题】
     * 这是创建所有棋子实体的核心辅助方法。
     * 它会加载棋子图片，并将其显示尺寸强制调整为与棋盘格大小相匹配。
     */
    private Entity newPiece(SpawnData data) {
        // 1. 从 SpawnData 中获取传递过来的逻辑棋子对象
        AbstractPiece pieceLogic = data.get("pieceLogic");

        // 2. 根据逻辑棋子，决定要加载哪张图片
        String textureName = getTextureName(pieceLogic);

        // 3. 将图片加载为一个 Texture 对象，而不是直接传文件名
        Texture pieceView = FXGL.getAssetLoader().loadTexture(textureName);


        int padding = 8;
        pieceView.setFitWidth(CELL_SIZE - padding);
        pieceView.setFitHeight(CELL_SIZE - padding);

        // (可选但强烈推荐) 保持图片原始的长宽比，防止棋子被压扁或拉长
        pieceView.setPreserveRatio(true);

        // 4. 【！！！关键步骤！！！】
        // 强制设置我们希望它显示的尺寸！
        // 无论原始图片多大，最终都会以这个尺寸显示。
        // 比格子稍微小一点会更好看，这里我们留出8像素的边距。

        StackPane view = new StackPane(pieceView);
        view.setPrefSize(CELL_SIZE - 8, CELL_SIZE - 8);




        // (可选但强烈推荐) 保持图片原始的长宽比，防止棋子被压扁或拉长

        // 5. 使用 entityBuilder 构建实体
        return entityBuilder(data)
                .type(EntityType.PIECE)
                // 使用我们已经调整好尺寸的 pieceView 对象作为视图和碰撞盒
                .viewWithBBox(view)
                .with(new PieceComponent(pieceLogic))
                .with(new VisualStateComponent())
                .collidable()
                .build();
    }

    /**
     * 将逻辑棋子对象映射到它的图片文件名。
     * 生成的名字将与你的文件名完全匹配 (e.g., "RedChariot.png").
     */
    private String getTextureName(AbstractPiece pieceLogic) {
        String colorPrefix = pieceLogic.isRed() ? "Red" : "Black";

        // 使用 getClass().getSimpleName() 来自动获取类名，例如 "ChariotPiece"
        // 然后去掉 "Piece" 后缀，得到 "Chariot"
        String pieceTypeName = pieceLogic.getClass().getSimpleName().replace("Piece", "");

        // 拼接成最终的文件名
        return colorPrefix + pieceTypeName + ".png";
    }
}