package controllers;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import application.Main;
public class ASLQuizController implements Initializable {
    @FXML
    private Label scoreLabel;
    @FXML
    private Label questionLabel;
    @FXML
    private ImageView imageView;
    @FXML
    private HBox optionsHBox;
    @FXML
    private Button nextButton;
    @FXML
    private StackPane imagePane;
    private final String IMAGE_RESOURCE_PATH = "/asl_alphabet_test";
    private int score = 0;
    private int questionIndex = 0;
    private List<Question> questions;
    private ToggleGroup optionsGroup;
    private RadioButton[] optionButtons;
    private final int NUM_OPTIONS = 4;
    private final int TOTAL_QUESTIONS = 10;
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupQuiz();
    }
    private void setupQuiz() {
         List<Question> generatedQuestions = null;
        try {
            generatedQuestions = generateQuestions();
        } catch (IOException e) {
            System.err.println("Error loading images or creating questions: " + e.getMessage()); // KEEP - Error message
            e.printStackTrace();
            showErrorDialog("Error", "Failed to load quiz data. Please check the image path and file names.");
            disableQuiz();
            return;
        }
        if (generatedQuestions == null || generatedQuestions.isEmpty()) {
            showErrorDialog("Error", "No questions were generated. Please check your data and image paths.");
            disableQuiz();
            return;
        }
        questions = generatedQuestions.stream().limit(TOTAL_QUESTIONS).collect(Collectors.toList());
        if(questions.size() < TOTAL_QUESTIONS){
            showErrorDialog("Error", "Not enough questions generated. Reduce TOTAL_QUESTIONS or add more images.");
            disableQuiz();
            return;
        }
        optionsGroup = new ToggleGroup();
        optionButtons = new RadioButton[NUM_OPTIONS];
        optionsHBox.setAlignment(Pos.CENTER);
        optionsHBox.getChildren().clear();
        for (int i = 0; i < NUM_OPTIONS; i++) {
            optionButtons[i] = new RadioButton();
            optionButtons[i].setToggleGroup(optionsGroup);
            optionsHBox.getChildren().add(optionButtons[i]);
        }
        nextButton.setOnAction(e -> {
            if (optionsGroup.getSelectedToggle() != null) {
                checkAnswer();
            } else {
                showAlert("Please select an answer.");
            }
        });
        scoreLabel.setText("Score: 0");
        questionIndex = 0;
        scoreLabel.getStyleClass().add("label");
        questionLabel.getStyleClass().add("label");
        nextButton.getStyleClass().add("button");
        loadQuestion();
    }
     private void disableQuiz() {
        questionLabel.setText("Quiz failed to load.");
        imageView.setVisible(false);
        imagePane.setVisible(false);
        imagePane.setManaged(false);
        optionsHBox.setVisible(false);
        optionsHBox.setManaged(false);
        nextButton.setVisible(false);
        nextButton.setManaged(false);
    }
    private List<Question> generateQuestions() throws IOException {
        List<Question> questionList = new ArrayList<>();
        List<String> alphabet = Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z");
         List<String> allPossibleLetters = new ArrayList<>(alphabet);

        Random random = new Random();
        List<String> imageFilenames = getImageFilenamesFromResources();
        if (imageFilenames.isEmpty()) {
             throw new IOException("No image files found or generated for resource path: " + IMAGE_RESOURCE_PATH);
        }
        Set<String> availableLetters = imageFilenames.stream()
                                      .map(ASLQuizController::extractLetterFromFilename)
                                      .filter(letter -> !letter.isEmpty())
                                      .collect(Collectors.toSet());
         if (availableLetters.isEmpty()) {
              throw new IOException("No valid letters extracted from image filenames.");
         }
        List<String> availableLettersList = new ArrayList<>(availableLetters);
        Collections.shuffle(availableLettersList);
        int lettersToUseCount = availableLettersList.size();
        int imageQuestionsToCreate = lettersToUseCount / 2;
        int letterQuestionsToCreate = lettersToUseCount - imageQuestionsToCreate;
         List<String> imagesForIL = new ArrayList<>(imageFilenames);
         Collections.shuffle(imagesForIL);
         List<String> lettersForIL = new ArrayList<>();
         List<String> selectedImageFilenamesForIL = new ArrayList<>();
         for(String filename : imagesForIL) {
             String letter = ASLQuizController.extractLetterFromFilename(filename);
             if(!letter.isEmpty() && availableLetters.contains(letter) && !lettersForIL.contains(letter)) {
                 lettersForIL.add(letter);
                 selectedImageFilenamesForIL.add(filename);
                 if(selectedImageFilenamesForIL.size() >= imageQuestionsToCreate) break;
             }
         }
        for (String imageFilename : selectedImageFilenamesForIL) {
            String correctAnswerLetter = ASLQuizController.extractLetterFromFilename(imageFilename);
            List<String> options = new ArrayList<>();
            options.add(correctAnswerLetter);
            List<String> incorrectLettersPool = new ArrayList<>(availableLettersList);
            incorrectLettersPool.remove(correctAnswerLetter);
            Collections.shuffle(incorrectLettersPool);
            options.addAll(incorrectLettersPool.subList(0, Math.min(NUM_OPTIONS - 1, incorrectLettersPool.size())));
            Collections.shuffle(options);
            questionList.add(new ImageToLetterQuestion(IMAGE_RESOURCE_PATH + "/" + imageFilename, correctAnswerLetter, options));
        }
        List<String> lettersForLI = new ArrayList<>(availableLettersList);
        lettersForLI.removeAll(lettersForIL);
        Collections.shuffle(lettersForLI);
        lettersForLI = lettersForLI.subList(0, Math.min(letterQuestionsToCreate, lettersForLI.size()));
        for (String correctAnswerLetter : lettersForLI) {
             String correctImageFilename = correctAnswerLetter + "_test.jpg";
             List<String> options = new ArrayList<>();
             options.add(correctImageFilename);
             List<String> incorrectFilenamesPool = new ArrayList<>(imageFilenames);
             incorrectFilenamesPool.remove(correctImageFilename);
             incorrectFilenamesPool.removeIf(fn -> ASLQuizController.extractLetterFromFilename(fn).equals(correctAnswerLetter));
             Collections.shuffle(incorrectFilenamesPool);
             options.addAll(incorrectFilenamesPool.subList(0, Math.min(NUM_OPTIONS - 1, incorrectFilenamesPool.size())));
             Collections.shuffle(options);
             questionList.add(new LetterToImageQuestion(correctAnswerLetter, options, imageFilenames));
        }
        Collections.shuffle(questionList);
        return questionList;
    }
    private List<String> getImageFilenamesFromResources() {
        List<String> filenames = new ArrayList<>();
        List<String> potentialBaseNames = new ArrayList<>();
        List<String> alphabet = Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z");
        alphabet.forEach(letter -> potentialBaseNames.add(letter));
        potentialBaseNames.add("del");
        potentialBaseNames.add("space");
        List<String> potentialFullFilenames = new ArrayList<>();
        for(String baseName : potentialBaseNames) {
            potentialFullFilenames.add(baseName + "_test.jpg");
        }
        for (String filename : potentialFullFilenames) {
             URL imageUrl = getClass().getResource(IMAGE_RESOURCE_PATH + "/" + filename);
             if (imageUrl != null) {
                 filenames.add(filename);
             }
        }
         if(filenames.isEmpty()){
             System.err.println("CRITICAL ERROR: No image files found in resources at " + IMAGE_RESOURCE_PATH); // KEEP
         }
        return filenames;
    }
    private static String extractLetterFromFilename(String filename) {
        String name = new File(filename).getName();
        if (name == null || name.isEmpty()) {
             return "";
        }
        int dotIndex = name.lastIndexOf('.');
        String baseName = (dotIndex == -1) ? name : name.substring(0, dotIndex);
        String lowerBaseName = baseName.toLowerCase();
        if (lowerBaseName.equals("del")) {
            return "DEL";
        }
        if (lowerBaseName.equals("space")) {
            return "SPACE";
        }
        int underscoreIndex = baseName.indexOf('_');
        String letterPart = (underscoreIndex == -1) ? baseName : baseName.substring(0, underscoreIndex);
        if (!letterPart.isEmpty()) {
             String firstChar = letterPart.substring(0, 1);
             if (firstChar.matches("[a-zA-Z]")) {
                  return firstChar.toUpperCase();
             }
        }
        return "";
    }
    private void loadQuestion() {
        if (questionIndex >= TOTAL_QUESTIONS) {
            showFinalScore();
            return;
        }
        if (questionIndex < questions.size()) {
            Question currentQuestion = questions.get(questionIndex);
            questionLabel.setText(currentQuestion.getQuestionText());
             for(int i = 0; i < NUM_OPTIONS; i++) {
                 RadioButton rb = optionButtons[i];
                 rb.setText(null);
                 rb.setGraphic(null);
                 rb.setUserData(null);
                 rb.setVisible(false);
                 rb.setManaged(false);
                 rb.getStyleClass().clear();
             }
            if (currentQuestion instanceof ImageToLetterQuestion) {
                 Image mainImage = currentQuestion.getImage();
                 if (mainImage != null && !mainImage.isError()) {
                     imageView.setImage(mainImage);
                 } else {
                     imageView.setImage(null);
                     System.err.println("ERROR: Failed to load main image for ImageToLetter question."); // KEEP
                 }
                imagePane.setVisible(true);
                imagePane.setManaged(true);
                imageView.setVisible(true);
            } else {
                imageView.setImage(null);
                imageView.setVisible(false);
                imagePane.setVisible(false);
                imagePane.setManaged(false);
            }
            optionsGroup.selectToggle(null);
            List<String> options = currentQuestion.getOptions();
            for (int i = 0; i < options.size() && i < NUM_OPTIONS; i++) {
                RadioButton rb = optionButtons[i];
                rb.setVisible(true);
                rb.setManaged(true);
                if (currentQuestion instanceof LetterToImageQuestion) {
                    String imageFilenameOption = options.get(i);
                    Image optionImage = getImageFromResource(IMAGE_RESOURCE_PATH + "/" + imageFilenameOption);
                    if (optionImage != null && !optionImage.isError()) {
                        ImageView optionImageView = new ImageView(optionImage);
                        optionImageView.setFitWidth(150);
                        optionImageView.setFitHeight(150);
                        rb.setGraphic(optionImageView);
                        rb.setText(null);
                        rb.setUserData(imageFilenameOption);
                        rb.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                         rb.getStyleClass().add("radio-button");
                         rb.getStyleClass().add("radio-button-image");
                    } else {
                         System.err.println("Q" + questionIndex + " ERROR: Failed to load image for option " + i + ": " + IMAGE_RESOURCE_PATH + "/" + imageFilenameOption); // KEEP
                         rb.setGraphic(null);
                         rb.setText("[Image Error]");
                         rb.setUserData(null);
                         rb.setContentDisplay(ContentDisplay.TEXT_ONLY);
                          rb.getStyleClass().add("radio-button");
                    }
                } else {
                    String letterOption = options.get(i);
                    rb.setText(letterOption);
                    rb.setGraphic(null);
                    rb.setUserData(null);
                    rb.setContentDisplay(ContentDisplay.TEXT_ONLY);
                     rb.getStyleClass().add("radio-button");
                }
            }
        }
    }
    private Image getImageFromResource(String resourcePath) {
         try {
            URL imageUrl = getClass().getResource(resourcePath);
            if (imageUrl == null) {
                 System.err.println("Error: Resource not found: " + resourcePath + " for Q" + questionIndex + "."); // KEEP
                 return null;
            }
            Image img = new Image(imageUrl.toExternalForm(), true);
             img.progressProperty().addListener((obs, oldProgress, newProgress) -> {
                 if (newProgress.doubleValue() >= 1.0) {
                      if (img.isError()) {
                          System.err.println("Async Error loading image from URL: " + imageUrl.toExternalForm() + " for Q" + questionIndex + "."); // KEEP
                      } else {
                      }
                 }
             });
             if (img.isError()) {
                 System.err.println("Initial error state loading image from URL: " + imageUrl.toExternalForm() + " for Q" + questionIndex + "."); // KEEP
                 return null;
             }
             return img;
         } catch (Exception e) {
             System.err.println("Error loading image resource: " + resourcePath + " - " + e.getMessage() + " for Q" + questionIndex + "."); // KEEP
             return null;
         }
    }
    @FXML
    private void checkAnswer() {
        Question currentQuestion = questions.get(questionIndex);
        RadioButton selectedButton = (RadioButton) optionsGroup.getSelectedToggle();
        if (selectedButton == null) {
            showAlert("Please select an answer.");
            return;
        }
        boolean isAnswerCorrect = false;
        if (currentQuestion instanceof LetterToImageQuestion) {
            String selectedImageFilename = (String) selectedButton.getUserData();
            if (selectedImageFilename != null) {
                 String selectedLetter = ASLQuizController.extractLetterFromFilename(selectedImageFilename);
                 isAnswerCorrect = currentQuestion.getCorrectAnswer().equalsIgnoreCase(selectedLetter);
            }
        } else {
            String selectedAnswerText = selectedButton.getText();
            isAnswerCorrect = currentQuestion.isCorrect(selectedAnswerText);
        }
        if (isAnswerCorrect) {
            score++;
            scoreLabel.setText("Score: " + score);
            showAlert("Correct!");
        } else {
            showIncorrectAlert(currentQuestion.getCorrectAnswer(), currentQuestion);
        }
        questionIndex++;
        loadQuestion();
    }
    private void showFinalScore() {
        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(20));
        
        Label titleLabel = new Label("Quiz Finished");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        Label scoreLabel = new Label("Your final score is: " + score + " out of " + TOTAL_QUESTIONS);
        scoreLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: white;");
        
        HBox buttonContainer = new HBox();
        buttonContainer.setAlignment(Pos.CENTER);
        
        Button leaveButton = new Button("Leave");
        leaveButton.setStyle("-fx-background-color: #8A2BE2; -fx-text-fill: white; "
                + "-fx-font-size: 14px; -fx-padding: 8px 20px; "
                + "-fx-background-radius: 15px; -fx-border-radius: 15px;");
        leaveButton.setOnAction(e -> {
            ((Stage) leaveButton.getScene().getWindow()).close();
            goBackToHome();
        });
        
        buttonContainer.getChildren().add(leaveButton);
        
        content.getChildren().addAll(titleLabel, scoreLabel, buttonContainer);
        
        Alert alert = new Alert(Alert.AlertType.NONE); 
        alert.setTitle("Quiz Results");
        alert.setHeaderText(null);
        alert.setGraphic(content);
        
        alert.getDialogPane().getButtonTypes().clear();
        
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStyleClass().add("dialog-pane");
        dialogPane.setPrefSize(400, 250);
        
        URL cssUrl = getClass().getResource("/views/application.css");
        if (cssUrl != null) {
            dialogPane.getStylesheets().add(cssUrl.toExternalForm());
        }
        
        alert.showAndWait();
    }
    private void showIncorrectAlert(String correctAnswerLetter, Question question) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Incorrect!");
        alert.setHeaderText(null);
        String message = "The correct answer was: " + correctAnswerLetter;
        ImageView correctImageView = null;
        if (question instanceof LetterToImageQuestion) {
            String correctImageFilename = correctAnswerLetter + "_test.jpg";
            Image correctAnswerImage = getImageFromResource(IMAGE_RESOURCE_PATH + "/" + correctImageFilename);
            if (correctAnswerImage != null && !correctAnswerImage.isError()) {
                 correctImageView = new ImageView(correctAnswerImage);
                 correctImageView.setFitWidth(100);
                 correctImageView.setFitHeight(100);
                 alert.setGraphic(correctImageView);
            } else {
                 message += " (Correct image '" + correctImageFilename + "' not found or failed to load)";
            }
             alert.setContentText(message);
        } else {
             alert.setGraphic(null);
             alert.setContentText(message);
        }
        DialogPane dialogPane = alert.getDialogPane();
        URL cssUrl = getClass().getResource("/views/application.css");
         if (cssUrl != null) {
            dialogPane.getStylesheets().add(cssUrl.toExternalForm());
         } else {
             System.err.println("Warning: application.css not found for dialog styling.");
         }
        alert.showAndWait();
    }
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("ASL Quiz");
        alert.setHeaderText(null);
        alert.setContentText(message);
        DialogPane dialogPane = alert.getDialogPane();
        URL cssUrl = getClass().getResource("/views/application.css");
         if (cssUrl != null) {
            dialogPane.getStylesheets().add(cssUrl.toExternalForm());
         } else {
             System.err.println("Warning: application.css not found for dialog styling.");
         }
        alert.showAndWait();
    }
     private void showErrorDialog(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        DialogPane dialogPane = alert.getDialogPane();
        URL cssUrl = getClass().getResource("/views/application.css");
         if (cssUrl != null) {
            dialogPane.getStylesheets().add(cssUrl.toExternalForm());
         } else {
             System.err.println("Warning: application.css not found for dialog styling.");
         }
        alert.showAndWait();
    }
    @FXML
    private void goBackToHome() {
        try {
            Main.changeScene("/views/learn.fxml");
        } catch (Exception e) {
            e.printStackTrace();
            showErrorDialog("Navigation Error", "Could not load the home page.");
        }
    }
    abstract static class Question {
        protected String correctAnswer;
        protected List<String> options;
        public Question(String correctAnswer, List<String> options) {
            this.correctAnswer = correctAnswer;
            this.options = options;
        }
        public abstract String getQuestionText();
        public abstract Image getImage();
        public String getCorrectAnswer() {
            return correctAnswer;
        }
        public List<String> getOptions() {
            return options;
        }
        public boolean isCorrect(String selectedAnswerText) {
            return correctAnswer.equalsIgnoreCase(selectedAnswerText);
        }
    }
    static class ImageToLetterQuestion extends Question {
        private final String imageResourcePath;
        public ImageToLetterQuestion(String imageResourcePath, String correctAnswer, List<String> options) {
            super(correctAnswer, options);
            this.imageResourcePath = imageResourcePath;
        }
        @Override
        public String getQuestionText() {
            return "Which letter is represented by the image?";
        }
        @Override
        public Image getImage() {
            try {
                 URL imageUrl = getClass().getResource(imageResourcePath);
                 if (imageUrl == null) {
                      System.err.println("Error: Resource not found for ImageToLetter question: " + imageResourcePath); // KEEP
                      return null;
                 }
                 Image img = new Image(imageUrl.toExternalForm(), true);
                 img.progressProperty().addListener((obs, oldProgress, newProgress) -> {
                     if (newProgress.doubleValue() >= 1.0) {
                          if (img.isError()) {
                              System.err.println("Async Error loading image from URL (ImageToLetter): " + imageUrl.toExternalForm()); // KEEP
                          } else {
                          }
                     }
                 });
                 if (img.isError()) {
                     System.err.println("Initial error state loading image from URL (ImageToLetter): " + imageUrl.toExternalForm()); // KEEP
                     return null;
                 }
                 return img;
            } catch (Exception e) {
                 System.err.println("Error loading question image (ImageToLetter): " + imageResourcePath + " - " + e.getMessage()); // KEEP
                return null;
            }
        }
    }
    static class LetterToImageQuestion extends Question {
        public LetterToImageQuestion(String letter, List<String> options, List<String> allImageFiles) {
            super(letter, options);
        }
        @Override
        public String getQuestionText() {
            return "Which image represents the letter: " + getCorrectAnswer() + "?";
        }
        @Override
        public Image getImage() {
            return null;
        }
    }
}