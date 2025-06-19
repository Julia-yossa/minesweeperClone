//package application;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import java.util.Random;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane; // We'll use this for layout
import javafx.util.Duration;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

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
    // --- Image Resources ---
    private Image mineImage;
    private Image flagImage;
    private Image boomImage; // For the mine you hit
    private Image winAlertImage; // Optional: for win/lose alert
    private Image loseAlertImage; // Optional: for win/lose alert

     // --- Timer Variables ---
    private Timeline timeline;
    private Label timerLabel;
    private int secondsElapsed;

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
                button.setGraphic(null); // Clear any existing graphic (like a flag)
                button.setText("");

                if (isMine) {
                   button.setGraphic(new ImageView(mineImage));
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
                button.setGraphic(null);
                button.setText(""); // Clear text if re-hiding (for reset)
            }
        }

        public boolean isFlagged() { return isFlagged; }
        public void setFlagged(boolean flagged) {
            isFlagged = flagged;
            if (flagged) {
                button.setGraphic(new ImageView(flagImage));
                button.setText(""); // Or use an icon for flag
                button.setStyle("-fx-background-color: #a0a0a0; -fx-border-color: #808080 #c0c0c0 #c0c0c0 #808080; -fx-border-width: 2; -fx-text-fill: orange;");
            } else {
                button.setGraphic(null);
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
        loadImages();
        setupTimer(); // Initialize timer

        gridPane = new GridPane();
        gridPane.setPadding(new Insets(10));
        gridPane.setHgap(2);
        gridPane.setVgap(2);

        initializeBoard();

        BorderPane root = new BorderPane(); // New root layout
        root.setTop(timerLabel); // Place timer at the top
        BorderPane.setAlignment(timerLabel, Pos.CENTER); // Center the timer label
        BorderPane.setMargin(timerLabel, new Insets(10)); // Add some margin
        root.setCenter(gridPane); // Place game board in the center

        Scene scene = new Scene(root, BOARD_SIZE * CELL_SIZE + 38, BOARD_SIZE * CELL_SIZE + 90); // Adjusted height for timer
        primaryStage.setTitle("Minesweeper!");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

     private void loadImages() {
        try {
            // Use getClass().getResourceAsStream() for resources bundled with your JAR
            mineImage = new Image(getClass().getResourceAsStream("resources/mine.png"), CELL_SIZE * 0.75, CELL_SIZE * 0.75, true, true);
            flagImage = new Image(getClass().getResourceAsStream("resources/flag.png"), CELL_SIZE * 0.75, CELL_SIZE * 0.75, true, true);
            boomImage = new Image(getClass().getResourceAsStream("resources/boom.png"), CELL_SIZE * 0.9, CELL_SIZE * 0.9, true, true); // Slightly larger for emphasis
            winAlertImage = new Image(getClass().getResourceAsStream("resources/win.png")); // Full size for alert
            loseAlertImage = new Image(getClass().getResourceAsStream("resources/lose.png")); // Full size for alert
        } catch (NullPointerException e) {
            System.err.println("Error loading image. Make sure 'resources' folder and image paths are correct.");
            e.printStackTrace();
            // Provide fallback text or handle the error gracefully
            mineImage = null; // Set to null or a placeholder
            flagImage = null;
            boomImage = null;
        }
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
            return;
        }

        if (firstClick) {
            ensureFirstClickSafe(cell.getRow(), cell.getCol());
            firstClick = false;
            timeline.play(); // Start the timer on the first click!
        }

        if (cell.isMine()) {
            cell.setRevealed(true);
            cell.button.setGraphic(new ImageView(boomImage));
            cell.button.setStyle("-fx-background-color: red; -fx-border-color: #d0d0d0; -fx-border-width: 1;");
            gameOver(false);
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
        timeline.stop(); // Stop the timer when the game ends!

        if (!won) {
        // If lost, reveal all mines (but only use the standard mine image)
            for (int r = 0; r < BOARD_SIZE; r++) {
                for (int c = 0; c < BOARD_SIZE; c++) {
                    Cell currentCell = board[r][c];
                    if (currentCell.isMine() && !currentCell.isRevealed()) { // Only reveal non-boom mines
                        currentCell.setRevealed(true); // This will set the regular mine image
                    } else if (currentCell.isFlagged() && !currentCell.isMine()) {
                        // Optional: Indicate incorrectly placed flags
                        currentCell.button.setText("X"); // Or a small red X image
                        currentCell.button.setStyle("-fx-background-color: lightcoral; -fx-border-color: #d0d0d0; -fx-border-width: 1; -fx-text-fill: white;");
                    }
                }
            }
        }


        String message = won ? "Congratulations! You won in " + secondsElapsed + " seconds!" : "Game Over! You hit a mine.";
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText(null);
        alert.setContentText(message + "\n\nClick OK to restart.");

        ImageView alertImageView = null;
        if (won && winAlertImage != null) {
            alertImageView = new ImageView(winAlertImage);
        } else if (!won && loseAlertImage != null) {
            alertImageView = new ImageView(loseAlertImage);
        }

        if (alertImageView != null) {
            alertImageView.setFitWidth(100); // Adjust size as needed
            alertImageView.setPreserveRatio(true);
            alert.setGraphic(alertImageView);
        }

        alert.showAndWait();

        resetGame();
    }

    private void resetGame() {
        firstClick = true;
        gameOver = false;
        revealedCells = 0;
        flagsPlaced = 0;
        timeline.stop(); // Stop the timer in case it was running
        secondsElapsed = 0; // Reset seconds
        timerLabel.setText("Time: 0"); // Update label

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

    private void setupTimer() {
        timerLabel = new Label("Time: 0");
        timerLabel.setFont(new Font(20)); // Make it visible

        secondsElapsed = 0;
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondsElapsed++;
            timerLabel.setText("Time: " + secondsElapsed);
        }));
        timeline.setCycleCount(Timeline.INDEFINITE); // Run indefinitely
    }


    public static void main(String[] args) {
        launch(args);
    }
}