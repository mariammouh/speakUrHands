package application;


import java.io.IOException;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.util.Duration;


public class Main extends Application {
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        try {
     
            
        	Image img = new Image(getClass().getResourceAsStream("/application/imagess/logo3.png"));
            
            stage.getIcons().add(img);

            primaryStage = stage; 
            changeScene("/views/home.fxml"); 
            primaryStage.setTitle("Speak your hands");
            
            primaryStage.setWidth(800);
            primaryStage.setHeight(600);
            primaryStage.show();

        
            Button startButton = (Button) primaryStage.getScene().lookup("#start");
            if (startButton != null) {
                applyFadeTransition(startButton); 
            }	
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    

    public static void changeScene(String fxmlFile) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource(fxmlFile));
        Scene scene = new Scene(fxmlLoader.load());
        scene.getStylesheets().add(Main.class.getResource("/views/application.css").toExternalForm());

        primaryStage.setScene(scene);
    }


    public void applyFadeTransition(Button node) {
        FadeTransition fade = new FadeTransition();
        fade.setDuration(Duration.seconds(1)); 
        fade.setFromValue(0);  
        fade.setToValue(1); 
        fade.setNode(node);    
        fade.play();         
    }

    public static void main(String[] args) {
        launch(args);
    }
}
