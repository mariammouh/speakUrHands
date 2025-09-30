package controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.application.Platform;
import application.Main;
import javafx.stage.Stage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Base64;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.scene.control.Alert;
import java.io.ByteArrayInputStream;

public class CameraController implements Initializable {

    @FXML private Label predictionLabel;
    @FXML private Label translationLabel;
    @FXML private ImageView cameraView;

    private Process pythonProcess;
    private BufferedReader pythonInputReader;
    private Thread pythonOutputReaderThread;
    private ExecutorService monitorExecutor;
    private Future<?> monitorFuture;
    private final AtomicBoolean isShuttingDownIntentionally = new AtomicBoolean(false);

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        predictionLabel.setText("Prediction: Waiting...");
        translationLabel.setText("Translation: ");
        startPythonProcess();
    }

    private void startPythonProcess() {
        isShuttingDownIntentionally.set(false);
        try {
            String pythonCommand = "python";
            String pythonScriptRelativePath = "src/controllers/asl_recognition.py";

            ProcessBuilder pb = new ProcessBuilder(pythonCommand, pythonScriptRelativePath);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            pythonProcess = pb.start();

            pythonInputReader = new BufferedReader(new InputStreamReader(pythonProcess.getInputStream()));

            pythonOutputReaderThread = new Thread(() -> {
                String line;
                try {
                    while (!isShuttingDownIntentionally.get() && (line = pythonInputReader.readLine()) != null) {
                        parsePythonOutput(line);
                    }
                } catch (IOException e) {
                    if (!isShuttingDownIntentionally.get()) {
                        showError("Error Reading Python Output", "Failed to read data from script: " + e.getMessage());
                    }
                } finally {
                    try {
                        if (pythonInputReader != null) pythonInputReader.close();
                    } catch (IOException ignored) {}
                }
            });
            pythonOutputReaderThread.setDaemon(true);
            pythonOutputReaderThread.start();

            monitorExecutor = Executors.newSingleThreadExecutor();
            monitorFuture = monitorExecutor.submit(() -> {
                try {
                    int exitCode = pythonProcess.waitFor();
                    if (exitCode != 0 && !isShuttingDownIntentionally.get()) {
                        showError("Python Script Error", "Script terminated unexpectedly (code: " + exitCode + ")");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    if (!isShuttingDownIntentionally.get()) {
                         showError("Process Monitoring Error", "Unexpected error waiting for script: " + e.getMessage());
                    }
                }
            });
        } catch (IOException e) {
            showError("Python Startup Error", "Could not start script: " + e.getMessage());
        }
    }

    private void parsePythonOutput(String line) {
        String[] parts = line.split(":", 2);
        if (parts.length != 2) return;

        String key = parts[0].trim();
        String value = parts[1].trim();

        Platform.runLater(() -> {
            switch (key) {
                case "FRAME_B64":
                    try {
                        byte[] imageBytes = Base64.getDecoder().decode(value);
                        Image fxImage = new Image(new ByteArrayInputStream(imageBytes));
                        cameraView.setImage(fxImage);
                    } catch (Exception e) {
                         if (!isShuttingDownIntentionally.get()) {
                             showError("Image Display Error", "Could not decode/display frame: " + e.getMessage());
                         }
                    }
                    break;
                case "PREDICTION":
                    predictionLabel.setText("Prediction: " + value);
                    break;
                case "TRANSLATION":
                    translationLabel.setText("Translation: " + value);
                    break;
            }
        });
    }

    @FXML
    private void goBackToHome() {
        isShuttingDownIntentionally.set(true);
        stopPythonProcess();
        try {
            Main.changeScene("/views/secondPage.fxml");
        } catch (Exception e) {
           showError("Navigation Error", "Could not load previous page: " + e.getMessage());
        }
    }

    private void stopPythonProcess() {
         isShuttingDownIntentionally.set(true);

        if (monitorFuture != null && !monitorFuture.isDone()) {
            monitorFuture.cancel(true);
        }
        if (monitorExecutor != null && !monitorExecutor.isShutdown()) {
            monitorExecutor.shutdownNow();
             try {
                monitorExecutor.awaitTermination(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (pythonProcess != null && pythonProcess.isAlive()) {
            pythonProcess.destroy();
            try {
                if (!pythonProcess.waitFor(1, TimeUnit.SECONDS)) {
                    pythonProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                pythonProcess.destroyForcibly();
                Thread.currentThread().interrupt();
            }
        }

        if (pythonOutputReaderThread != null && pythonOutputReaderThread.isAlive()) {
             pythonOutputReaderThread.interrupt();
        }

         try {
            if (pythonInputReader != null) pythonInputReader.close();
         } catch (IOException ignored) {}
    }

    private void showError(String title, String message) {
        if (!isShuttingDownIntentionally.get()) {
             Platform.runLater(() -> {
                 Alert alert = new Alert(Alert.AlertType.ERROR);
                 alert.setTitle(title);
                 alert.setHeaderText(null);
                 alert.setContentText(message);
                 alert.showAndWait();
             });
        }
    }

    public void shutdown() {
        isShuttingDownIntentionally.set(true);
        stopPythonProcess();
    }

    public void setPrimaryStage(Stage stage) {
        if (stage != null) {
            stage.setOnCloseRequest(event -> {
                 shutdown();
            });
        }
    }
}