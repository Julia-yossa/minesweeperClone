//package application;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import java.util.Random;

public class App extends Application {

    // --- Game Constants ---
    private static final int BOARD_SIZE = 10;
    private static final int NUM_MINES = 15; // Adjust difficulty
    private static final int CELL_SIZE = 40; // Size of each button/cell

    // --- Game State Variables ---
    private Cell[][] board = new Cell[BOARD_SIZE][BOARD_SIZE];
    private boolean[][] mines = new boolean[BOARD_SIZE][BOARD_SIZE];
    private int[][] neighborMineCounts = new int[BOARD_SIZE][BOARD_SIZE];
    private boolean firstClick = true; // To ensure the first click is never a mine
    private int revealedCells = 0;
    private int flagsPlaced = 0;
    private boolean gameOver = false;

    private GridPane gridPane; // To hold our buttons

    // Inner class to represent a single cell on the board
    private class Cell extends StackPane {
        private int row;
        private int col;
        private Button button; // The visible button for the cell
        private boolean isMine;
        private boolean isRevealed = false;
        private boolean isFlagged = false;
        private int mineCount = 0; // Number of neighboring mines

        public Cell(int row, int col) {
            this.row = row;
            this.col = col;

            button = new Button("");
            button.setMinSize(CELL_SIZE, CELL_SIZE);
            button.setMaxSize(CELL_SIZE, CELL_SIZE);
            button.setFont(new Font(18)); // Adjust font size for numbers

            // Initial styling for unrevealed cells
            button.setStyle("-fx-background-color: #a0a0a0; -fx-border-color: #808080 #c0c0c0 #c0c0c0 #808080; -fx-border-width: 2;");

            // Event handlers (we'll implement these later)
            button.setOnMouseClicked(e -> {
                if (!gameOver) {
                    if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                        // Left click
                        handleLeftClick(this);
                    } else if (e.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                        // Right click
                        handleRightClick(this);
                    }
                }
            });

            this.getChildren().add(button);
        }

        // --- Getters and Setters ---
        public boolean isRevealed() { return isRevealed; }
        public void setRevealed(boolean revealed) {
            isRevealed = revealed;
            if (revealed) {
                // Change appearance when revealed
                button.setStyle("-fx-background-color: #e0e0e0; -fx-border-color: #d0d0d0; -fx-border-width: 1;");
                if (isMine) {
                    button.setText("M"); // Or use an icon for mine
                    button.setStyle("-fx-background-color: red; -fx-border-color: #d0d0d0; -fx-border-width: 1;");
                } else if (mineCount > 0) {
                    button.setText(String.valueOf(mineCount));
                    // Style numbers differently
                    String textColor = "";
                    switch (mineCount) {
                        case 1: textColor = "blue"; break;
                        case 2: textColor = "green"; break;
                        case 3: textColor = "red"; break;
                        case 4: textColor = "purple"; break;
                        case 5: textColor = "maroon"; break;
                        case 6: textColor = "teal"; break;
                        case 7: textColor = "black"; break;
                        case 8: textColor = "gray"; break;
                    }
                    button.setStyle(button.getStyle() + String.format("-fx-text-fill: %s;", textColor));
                } else {
                    button.setText(""); // Empty for 0 mines
                }
            } else {
                // Reset to unrevealed style
                button.setStyle("-fx-background-color: #a0a0a0; -fx-border-color: #808080 #c0c0c0 #c0c0c0 #808080; -fx-border-width: 2;");
                button.setText(""); // Clear text if re-hiding (for reset)
            }
        }

        public boolean isFlagged() { return isFlagged; }
        public void setFlagged(boolean flagged) {
            isFlagged = flagged;
            if (flagged) {
                button.setText("F"); // Or use an icon for flag
                button.setStyle("-fx-background-color: #a0a0a0; -fx-border-color: #808080 #c0c0c0 #c0c0c0 #808080; -fx-border-width: 2; -fx-text-fill: orange;");
            } else {
                button.setText("");
                // Reset style if unflagged
                button.setStyle("-fx-background-color: #a0a0a0; -fx-border-color: #808080 #c0c0c0 #c0c0c0 #808080; -fx-border-width: 2;");
            }
        }

        public boolean isMine() { return isMine; }
        public void setMine(boolean mine) { isMine = mine; }
        public int getMineCount() { return mineCount; }
        public void setMineCount(int count) { this.mineCount = count; }
        public int getRow() { return row; }
        public int getCol() { return col; }
    }

    @Override
    public void start(Stage primaryStage) {
        gridPane = new GridPane();
        gridPane.setPadding(new Insets(10));
        gridPane.setHgap(2); // Small gap between cells
        gridPane.setVgap(2);

        initializeBoard(); // Call our board setup method

        Scene scene = new Scene(gridPane, BOARD_SIZE * CELL_SIZE + 20, BOARD_SIZE * CELL_SIZE + 20); // Add padding
        primaryStage.setTitle("Minesweeper!");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    // --- Game Logic Methods (to be implemented next) ---
    private void initializeBoard() {
        // Create cells and add them to the GridPane
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                Cell cell = new Cell(row, col);
                board[row][col] = cell;
                gridPane.add(cell, col, row); // Add to GridPane (col, row)
            }
        }
        // Mines are placed *after* the first click to ensure the first click is safe.
        // For now, we'll initialize them immediately for simplicity in initial testing.
        // We'll modify this for the first click logic later.
        placeMines();
        calculateNeighborMineCounts();
    }

    private void placeMines() {
        Random random = new Random();
        int minesPlaced = 0;
        while (minesPlaced < NUM_MINES) {
            int r = random.nextInt(BOARD_SIZE);
            int c = random.nextInt(BOARD_SIZE);

            if (!mines[r][c]) { // If no mine is already there
                mines[r][c] = true;
                board[r][c].setMine(true); // Mark cell as mine
                minesPlaced++;
            }
        }
    }

    private void calculateNeighborMineCounts() {
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                if (!board[r][c].isMine()) { // Only count for non-mine cells
                    int count = 0;
                    // Check all 8 neighbors
                    for (int dr = -1; dr <= 1; dr++) {
                        for (int dc = -1; dc <= 1; dc++) {
                            if (dr == 0 && dc == 0) continue; // Skip self

                            int nr = r + dr;
                            int nc = c + dc;

                            // Check bounds
                            if (nr >= 0 && nr < BOARD_SIZE && nc >= 0 && nc < BOARD_SIZE) {
                                if (mines[nr][nc]) {
                                    count++;
                                }
                            }
                        }
                    }
                    board[r][c].setMineCount(count);
                    neighborMineCounts[r][c] = count; // Also store in a separate array for convenience
                }
            }
        }
    }

    // --- Event Handlers (to be implemented) ---
    private void handleLeftClick(Cell cell) {
        if (cell.isRevealed() || cell.isFlagged() || gameOver) {
            return; // Do nothing if already revealed, flagged, or game is over
        }

        if (firstClick) {
            // Re-place mines to ensure first click is never a mine and is a 0
            ensureFirstClickSafe(cell.getRow(), cell.getCol());
            firstClick = false;
        }

        if (cell.isMine()) {
            cell.setRevealed(true); // Show the mine
            gameOver(false); // Game over - lost
            return;
        }

        revealCell(cell.getRow(), cell.getCol());
        checkWinCondition();
    }

    private void handleRightClick(Cell cell) {
        if (cell.isRevealed() || gameOver) {
            return; // Cannot flag revealed cells or if game is over
        }

        if (cell.isFlagged()) {
            cell.setFlagged(false);
            flagsPlaced--;
        } else {
            // Optional: Limit flags to NUM_MINES
            // if (flagsPlaced < NUM_MINES) {
                cell.setFlagged(true);
                flagsPlaced++;
            // }
        }
    }

    // Recursive function to reveal cells
    private void revealCell(int r, int c) {
        if (r < 0 || r >= BOARD_SIZE || c < 0 || c >= BOARD_SIZE) {
            return; // Out of bounds
        }
        Cell cell = board[r][c];
        if (cell.isRevealed() || cell.isMine() || cell.isFlagged()) {
            return; // Already revealed, is a mine, or flagged
        }

        cell.setRevealed(true);
        revealedCells++;

        if (cell.getMineCount() == 0) {
            // Recursively reveal neighbors if current cell has 0 neighboring mines
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if (dr == 0 && dc == 0) continue;
                    revealCell(r + dr, c + dc);
                }
            }
        }
    }

    private void ensureFirstClickSafe(int clickedRow, int clickedCol) {
        // Reset mines and counts
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                mines[r][c] = false;
                board[r][c].setMine(false);
                board[r][c].setMineCount(0); // Reset count
            }
        }

        Random random = new Random();
        int minesPlaced = 0;
        while (minesPlaced < NUM_MINES) {
            int r = random.nextInt(BOARD_SIZE);
            int c = random.nextInt(BOARD_SIZE);

            // Ensure the mine is not at the clicked location or its immediate neighbors
            boolean isTooCloseToFirstClick = false;
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if (r == clickedRow + dr && c == clickedCol + dc) {
                        isTooCloseToFirstClick = true;
                        break;
                    }
                }
                if (isTooCloseToFirstClick) break;
            }

            if (!mines[r][c] && !isTooCloseToFirstClick) {
                mines[r][c] = true;
                board[r][c].setMine(true);
                minesPlaced++;
            }
        }
        calculateNeighborMineCounts(); // Recalculate based on new mine positions
    }


    private void checkWinCondition() {
        // Win if all non-mine cells are revealed
        if (revealedCells == (BOARD_SIZE * BOARD_SIZE - NUM_MINES)) {
            gameOver(true); // Game over - won
        }
    }

    private void gameOver(boolean won) {
        gameOver = true;
        String message = won ? "Congratulations! You won!" : "Game Over! You hit a mine.";
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText(null);
        alert.setContentText(message + "\n\nClick OK to restart.");
        alert.showAndWait();

        // Restart the game
        resetGame();
    }

    private void resetGame() {
        firstClick = true;
        gameOver = false;
        revealedCells = 0;
        flagsPlaced = 0;

        // Clear existing mines and states
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                Cell cell = board[r][c];
                cell.setRevealed(false);
                cell.setFlagged(false);
                cell.setMine(false);
                cell.setMineCount(0);
                mines[r][c] = false;
                neighborMineCounts[r][c] = 0;
            }
        }
        // Mines and counts will be placed after the first click
        // For now, no mines are placed
    }


    public static void main(String[] args) {
        launch(args);
    }
}