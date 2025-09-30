package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.stage.Stage; 

import application.Main; 


public class LearnController {


    @FXML
    private Button backButton;
    @FXML
    private Button startLearnButton; 


    @FXML
    private void goBackToHome() {
        try {
       
            Main.changeScene("/views/secondPage.fxml"); 
        } catch (Exception e) {
            e.printStackTrace();
         
             showErrorDialog("Navigation Error", "Could not load the second page.");
        }
    }


    @FXML 
    private void startLearning() {
        try {
          
            Main.changeScene("/views/asl_quiz.fxml");
        } catch (Exception e) {
            e.printStackTrace();
            showErrorDialog("Loading Error", "Failed to load the ASL Quiz.");
        }
    }


    private void showErrorDialog(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/application.css").toExternalForm());

        alert.showAndWait();
    }
    
}