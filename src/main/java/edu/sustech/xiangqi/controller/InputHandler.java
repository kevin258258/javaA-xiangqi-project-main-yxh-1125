package edu.sustech.xiangqi.controller;

import edu.sustech.xiangqi.model.ChessBoardModel;
import javafx.geometry.Point2D;
import static edu.sustech.xiangqi.XiangQiApp.*; // 静态导入我们所有的布局常量

import edu.sustech.xiangqi.model.ChessBoardModel;
import javafx.geometry.Point2D;
import static edu.sustech.xiangqi.XiangQiApp.*; // 静态导入我们所有的布局常量

public class InputHandler {

    private final boardController boardController;

    public InputHandler(boardController boardController) {
        this.boardController = boardController;
    }

    /**
     * 处理在指定屏幕位置的鼠标点击事件。
     * 它的核心工作是将屏幕像素坐标转换为逻辑网格坐标。
     *
     * @param screenPosition 鼠标点击的屏幕像素坐标。
     */
    public void handleMouseClick(Point2D screenPosition) {
        // 1. 【边界检查】
        // 首先，我们检查点击是否在棋盘的有效区域内，忽略在UI区或窗口边缘的点击。
        double clickX = screenPosition.getX();
        double clickY = screenPosition.getY();

        // 【修改点】使用新的 BOARD_START_X 变量来判断边界
        if (clickX < BOARD_START_X || clickX > BOARD_START_X + BOARD_WIDTH ||
                clickY < 0 || clickY > BOARD_HEIGHT) {
            return; // 点击在棋盘之外，直接忽略
        }

        // 2. 【坐标转换】
        // 计算点击位置相对于棋盘网格左上角(0,0)点的像素偏移量。
        // 棋盘网格的左上角 = 棋盘起始X + 棋盘图片内部边距
        // 【修改点】使用 BOARD_START_X
        double xInGrid = clickX - (BOARD_START_X + MARGIN);
        double yInGrid = clickY - MARGIN;

        // 3. 【计算行列号】
        // 将像素偏移量除以每个格子的大小，并四舍五入到最近的整数，得到行列号。
        int col = Math.round((float) (xInGrid / CELL_SIZE));
        int row = Math.round((float) (yInGrid / CELL_SIZE));

        // 4. 【合法性验证】
        // 确保计算出的行列号在棋盘的有效范围内 (e.g., row 0-9, col 0-8)
        if (row < 0 || row >= ChessBoardModel.getRows() || col < 0 || col >= ChessBoardModel.getCols()) {
            return; // 点击位置虽然在棋盘图片上，但在网格之外，忽略
        }

        // 5. 【委托指令】
        // 将翻译好的、干净的逻辑坐标 (row, col) 交给总控制器去处理后续的游戏逻辑。
        boardController.onGridClicked(row, col);
    }
}