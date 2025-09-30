import numpy as np
import tensorflow as tf
import cv2
import os 
import time

IMAGE_SIZE = (128, 128)
CLASS_NAMES = [chr(i) for i in range(ord('A'), ord('Z') + 1)] + ['nothing', 'del', 'space']
MODEL_PATH = 'best_model.keras'
CAPTURE_INTERVAL = 10  

def load_model_with_validation():
    """Loads the Keras model and handles potential loading errors."""
    try:
        model = tf.keras.models.load_model(MODEL_PATH)
        print("✅ Model loaded successfully")
        return model
    except Exception as e:
        print(f"❌ Error loading model: {str(e)}")
        exit(1) 

def preprocess_frame(frame):
    """Resizes and normalizes a frame for model input."""
    try:
        img = cv2.resize(frame, IMAGE_SIZE)
        img = img.astype(np.float32) / 255.0 
        return np.expand_dims(img, axis=0) 
    except Exception as e:
        raise RuntimeError(f"Preprocessing error: {str(e)}")

def predict_from_frame(model, frame):
    """Processes a frame and returns the model's prediction and confidence."""
    processed_img = preprocess_frame(frame)
    preds = model.predict(processed_img)
    pred_index = np.argmax(preds)
    confidence = preds[0][pred_index]
    return CLASS_NAMES[pred_index], confidence

# --- Real-Time Detection Logic ---
def realtime_detection():
    """Handles webcam capture, prediction scheduling, and display."""
    model = load_model_with_validation()
    cap = cv2.VideoCapture(0) 

    if not cap.isOpened():
        print("❌ Cannot access webcam")
        return

    print("📷 Press 'q' to quit")
    print(f"📸 Capturing image every {CAPTURE_INTERVAL} seconds...")

    last_capture_time = time.time()
    current_prediction = None
    current_confidence = None
    translation_history = [] 

    while True:
        ret, frame = cap.read()
        if not ret:
            print("❌ Failed to capture frame")
            break

        frame = cv2.flip(frame, 1) 
        current_time = time.time()

        if current_time - last_capture_time >= CAPTURE_INTERVAL:
            try:
                current_prediction, current_confidence = predict_from_frame(model, frame)
                timestamp = time.strftime('%H:%M:%S')
                print(f"📸 Capture at {timestamp}: {current_prediction} ({current_confidence:.1%})")

                if current_prediction == 'nothing':
                    pass
                elif current_prediction == 'del':
                    if translation_history: translation_history.pop()
                elif current_prediction == 'space':
                    translation_history.append(' ')
                else:
                    translation_history.append(current_prediction)

                last_capture_time = current_time 
            except Exception as e:
                print(f"❌ Prediction error: {str(e)}")
                current_prediction = "Error"
                current_confidence = 0.0

        if current_prediction:
            label = f"Prediction: {current_prediction} ({current_confidence:.1%})"
            cv2.putText(frame, label, (10, 40), cv2.FONT_HERSHEY_SIMPLEX, 1.2, (0, 255, 0), 2)

        translation_text = "Translation: " + ''.join(translation_history)
        cv2.putText(frame, translation_text, (10, 80), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 255), 2)

        time_until_next = CAPTURE_INTERVAL - (current_time - last_capture_time)
        countdown_text = f"Next capture in: {max(0, int(time_until_next))}s"
        cv2.putText(frame, countdown_text, (10, 120), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 255), 2)

        cv2.imshow("ASL Recognition - Webcam", frame)

        # Quit if 'q' is pressed
        if cv2.waitKey(1) & 0xFF == ord('q'):
            break

    cap.release()
    cv2.destroyAllWindows()

    print("\nFinal Translation:")
    print(''.join(translation_history))

if __name__ == "__main__":
    realtime_detection()