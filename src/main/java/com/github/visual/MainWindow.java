package com.github.visual;

import com.github.fx.DoubleTextField;
import com.github.fx.ScaleEventHandler;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import com.github.types.Complex;
import com.github.types.View;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Stack;

import static javafx.embed.swing.SwingFXUtils.toFXImage;


/**
 * THE FOLLOWING PROGRAM EXAMPLE WAS MADE BY TIM KOCHETKOV WITH ASSISTANCE OF Michael B., his dear friend.
 * Michael's GitHub --- https://github.com/KolyanPie
 * TIM's GitHub --- https://github.com/TimofeyKochetkov
 */

public class MainWindow extends Application {
    public AnchorPane biggestAnchorPaneInTheWorld;
    private Stack<View> history;
    private Stack<View> undoHistory;
    private PrintTask task;

    @FXML
    private Pane pane;
    @FXML
    private ImageView imageView;
    @FXML
    private Canvas canvas;
    @FXML
    private DoubleTextField textX1;
    @FXML
    private DoubleTextField textX2;
    @FXML
    private DoubleTextField textY1;
    @FXML
    private DoubleTextField textY2;
    @FXML
    private TextField textMaxIter;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label progressLabel;
    @FXML
    private Button buttonUndo;
    @FXML
    private Button buttonRedo;

    private BufferedImage bi;
    private double imageWidth;
    private double imageHeight;

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setOnCloseRequest(t -> {
            Platform.exit();
            System.exit(0);
        });
        primaryStage.setResizable(false);
        primaryStage.setTitle("Main Render");
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/main_window.fxml"));
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public void initialize() {
        history = new Stack<>();
        undoHistory = new Stack<>();
        task = new PrintTask();
        canvas.setWidth(900);
        canvas.setHeight(600);
        System.out.println(pane.widthProperty());
        System.out.println(biggestAnchorPaneInTheWorld.widthProperty());
        onClickPrint();
        canvas.getGraphicsContext2D().setStroke(javafx.scene.paint.Color.GREEN);
        ScaleEventHandler eventHandler = new ScaleEventHandler(this::scale, canvas);
        canvas.setOnMousePressed(eventHandler);
        canvas.setOnMouseDragged(eventHandler);
        canvas.setOnMouseReleased(eventHandler);
    }

    private void scale(double x1, double x2, double y1, double y2) {
        View view = history.peek();

        x1 = x1 * (view.x2 - view.x1) / this.imageWidth + view.x1;
        x2 = x2 * (view.x2 - view.x1) / this.imageWidth + view.x1;
        y1 = y1 * (view.y2 - view.y1) / this.imageHeight + view.y1;
        y2 = y2 * (view.y2 - view.y1) / this.imageHeight + view.y1;

        setCoordsAndPrint(x1, x2, y1, y2, view.maxIter);
    }

    private void setCoordsAndPrint(double x1, double x2, double y1, double y2, int maxIter) {
        View item = new View(x1, x2, y1, y2, maxIter);
        if (history.size() > 0 && history.peek().equals(item)) {
            return;
        }
        history.push(item);
        if (history.size() > 1) {
            buttonUndo.setDisable(false);
        }
        undoHistory.clear();
        buttonRedo.setDisable(true);
        printSet();
    }

    private void printSet() {
        task.cancel();
        progressLabel.setText("processing...");
        View view = history.peek();
        textX1.setDouble(view.x1);
        textX2.setDouble(view.x2);
        textY1.setDouble(view.y1);
        textY2.setDouble(view.y2);
        textMaxIter.setText(String.valueOf(view.maxIter));
        task = new PrintTask();
        task.setOnSucceeded(event -> {
            imageView.setImage(toFXImage(bi, null));
            progressLabel.setText("success");
        });
        task.setOnFailed(event -> {
            throw new RuntimeException(task.getException());
        });
        task.setOnCancelled(event -> progressLabel.setText("cancelled"));
        new Thread(task).start();
    }

    /**
     *
     * @param complex - the complex nmber that is fed into an iterative function
     * @param maxIter - number of final iteration, after which the drawing process is finished
     * @return number of "jumps" that it takes for a complex number after iterating over equation
     * for the Mandelbrot Set (Zn+1=Zn^2+C, where C=x + i* y - a random select point for the part of the complex plane
     * (which belongs to the fractal, of course)
     *
     * THE FRACTAL RECTANGULAR BOUNDARIES CAN BE DESCRIBED BY A FOLLOWING PAIR OF POINTS (bot-left and top-right)
     * { {-2;-1} ; {-1;1} } )
     * The resulting color of the point depends on the number of jumps that it takes for this point
     * after iterating it's coordinates to jump BEYOND SAID BOUNDARIES ^^^
     */

    private int jumpCount(Complex complex, int maxIter) {
        Complex initialComplex = complex.clone();
        int count = 0;
        double module = 0;
        while ((module <= 4) && (count <= maxIter)) {
            module = complex.module_square();
            complex.multiply(complex).add(initialComplex);
            count++;
        }
        return count;
    }

    @FXML
    private void onClickPrint() {
        setCoordsAndPrint(textX1.getDouble(),
                textX2.getDouble(),
                textY1.getDouble(),
                textY2.getDouble(), Integer.parseInt(textMaxIter.getText()));
    }


    @FXML
    private void onClickPrintReset() {
        textX1.setDouble(-2.0);
        textX2.setDouble(1.0);
        textY1.setDouble(-1.0);
        textY2.setDouble(1.0);
        textMaxIter.setText("50");
        onClickPrint();
    }

    @FXML
    private void onClickUndo() {
        undoHistory.push(history.pop());
        buttonRedo.setDisable(false);
        if (history.size() == 1) {
            buttonUndo.setDisable(true);
        }
        printSet();
    }

    @FXML
    private void onClickRedo() {
        history.push(undoHistory.pop());
        if (undoHistory.size() == 0) {
            buttonRedo.setDisable(true);
        }
        buttonUndo.setDisable(false);
        printSet();
    }

    private class PrintTask extends Task<Void> {
        @Override
        protected Void call() {
            View view = history.peek();
            Color color;
            Complex temp;
            int jumps;
            double aspectRatio = (view.x2 - view.x1) / (view.y2 - view.y1);
            imageWidth = canvas.widthProperty().getValue();
            imageHeight = canvas.heightProperty().getValue();
            if (imageWidth > aspectRatio * imageHeight) {
                imageWidth = (aspectRatio * imageHeight);
            } else {
                imageHeight = (imageWidth / aspectRatio);
            }
            bi = new BufferedImage((int) imageWidth, (int) imageHeight, BufferedImage.TYPE_INT_RGB);
            int iterCount = 0;
            for (double x = view.x1; x < view.x2; x += (view.x2 - view.x1) / imageWidth) {
                for (double y = view.y1; y < view.y2; y += (view.y2 - view.y1) / imageHeight) {
                    temp = new Complex(x, y);
                    jumps = jumpCount(temp, view.maxIter);
                    if (jumps < view.maxIter) {
                        //TODO: variable color with a broad spectrum
                        color = new Color((jumps * 255 / view.maxIter), 70, 255);
                    } else color = Color.black;
                    try {
                        bi.setRGB((int) Math.round((x - view.x1) * imageWidth / (view.x2 - view.x1)),
                                (int) Math.round((y - view.y1) * imageHeight / (view.y2 - view.y1)),
                                color.getRGB());
                    } catch (ArrayIndexOutOfBoundsException e) {
                        //FixMe: when crossing a border of the picture with a selection frame, some points are missdrawn

                        System.out.println(x);
                        System.out.println(y);
                        System.out.println((int) Math.round((x - view.x1) * imageWidth / (view.x2 - view.x1)));
                        System.out.println((int) Math.round((y - view.y1) * imageHeight / (view.y2 - view.y1)));
                    }
                    progressBar.setProgress((double) iterCount++ / (imageWidth * imageHeight));
                    if (isCancelled()) {
                        return null;
                    }
                }
            }
            return null;
        }
    }
}





