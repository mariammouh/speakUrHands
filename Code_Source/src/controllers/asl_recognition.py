import numpy as np
import tensorflow as tf
import cv2
import os
import time
import sys
import base64

IMAGE_SIZE = (128, 128)
CLASS_NAMES = [chr(i) for i in range(ord('A'), ord('Z') + 1)] + ['nothing', 'del', 'space']

MODEL_PATH = 'training/best_model.keras'
PREDICTION_INTERVAL = 5 

def load_model():
    """Load and return the trained TensorFlow Keras model."""
    abs_model_path = os.path.abspath(MODEL_PATH)
    if not os.path.exists(abs_model_path):
        print(f"STATUS:Error - Model file not found at {abs_model_path}", file=sys.stderr)
        sys.stderr.flush()
        return None

    try:
        model = tf.keras.models.load_model(abs_model_path)
        print(f"STATUS:Model loaded successfully from {abs_model_path}", file=sys.stderr)
        sys.stderr.flush()
        return model
    except Exception as e:
        print(f"STATUS:Error - Failed to load model: {e}", file=sys.stderr)
        sys.stderr.flush()
        return None

def preprocess_frame(frame):
    """Preprocess a single camera frame for model prediction."""
    try:
        img = cv2.resize(frame, IMAGE_SIZE)
        img = img.astype(np.float32) / 255.0  
        return np.expand_dims(img, axis=0) 
    except Exception as e:
        print(f"STATUS:Error - Frame preprocessing failed: {e}", file=sys.stderr)
        sys.stderr.flush()
        return None

def predict_from_frame(model, frame):
    """Predict hand gesture from a frame using the loaded model."""
    try:
        processed_img = preprocess_frame(frame)
        if processed_img is None:
            return "Error", 0.0

        predictions = model.predict(processed_img, verbose=0)
        predicted_index = np.argmax(predictions)
        confidence = float(predictions[0][predicted_index])
        predicted_class = CLASS_NAMES[predicted_index]
        return predicted_class, confidence
    except Exception as e:
        print(f"STATUS:Error - Prediction failed: {e}", file=sys.stderr)
        sys.stderr.flush()
        return "Error", 0.0

def handle_prediction_result(prediction, translation_history):
    """Update the translation history based on the prediction result."""
    if prediction == 'del' and translation_history:
        translation_history.pop()
    elif prediction == 'space':
        if not translation_history or translation_history[-1] != ' ':
            translation_history.append(' ')
    elif prediction not in ['nothing', 'del', 'space']:

         last_meaningful_char = next((char for char in reversed(translation_history) if char not in (' ', 'nothing', 'del')), None)
         if prediction != last_meaningful_char:
             translation_history.append(prediction)

    return translation_history 

def realtime_detection_for_java():
    """Main loop for camera capture, processing, prediction, and communication with Java."""
    model = load_model()
    if model is None:
        print("STATUS:Error - Exiting due to model load failure.", file=sys.stderr)
        sys.stderr.flush()
        time.sleep(5) 
        sys.exit(1)

    cap = cv2.VideoCapture(0)
    if not cap.isOpened():
        print("STATUS:Error - Cannot access camera.", file=sys.stderr)
        sys.stderr.flush()
        time.sleep(5)
        sys.exit(1)

    print("STATUS:Camera opened. Starting detection loop.", file=sys.stderr)
    sys.stderr.flush()

    last_prediction_time = time.time()
    translation_history = []

    try:
        while True:
            ret, frame = cap.read()
            if not ret:
                print("STATUS:Warning - Failed to grab frame. Retrying...", file=sys.stderr)
                sys.stderr.flush()
                time.sleep(0.1)
                if not cap.isOpened():
                    print("STATUS:Error - Camera became inaccessible.", file=sys.stderr)
                    sys.stderr.flush()
                    break 
                continue

            frame = cv2.flip(frame, 1) 
            current_time = time.time()

            try:
                ret_encode, buffer = cv2.imencode('.jpg', frame, [int(cv2.IMWRITE_JPEG_QUALITY), 80])
                if not ret_encode:
                    print("STATUS:Warning - Failed to encode frame.", file=sys.stderr)
                    sys.stderr.flush()
                    continue

                frame_base64 = base64.b64encode(buffer.tobytes()).decode('utf-8')

                print(f"FRAME_B64:{frame_base64}")
                sys.stdout.flush() 
            except Exception as e:
                 print(f"STATUS:Error - Failed during frame encoding/sending: {e}", file=sys.stderr)
                 sys.stderr.flush()

            if current_time - last_prediction_time >= PREDICTION_INTERVAL:
                prediction, confidence = predict_from_frame(model, frame)

                if prediction != "Error":
                     translation_history = handle_prediction_result(prediction, translation_history)

               
                print(f"PREDICTION:{prediction}")

                display_translation = ''.join([c for c in translation_history if c not in ('nothing', 'del')])
                print(f"TRANSLATION:{display_translation}")
                sys.stdout.flush() 

                last_prediction_time = current_time

            time.sleep(0.02) 

    except KeyboardInterrupt:
        print("STATUS:Received KeyboardInterrupt. Shutting down.", file=sys.stderr)
        sys.stderr.flush()
    except Exception as e:
         print(f"STATUS:Error - An unexpected error occurred in the main loop: {e}", file=sys.stderr)
         sys.stderr.flush()
    finally:
        print("STATUS:Initiating shutdown sequence.", file=sys.stderr)
        if cap.isOpened():
            cap.release()
            print("STATUS:Camera released.", file=sys.stderr)
        print("STATUS:Shutdown complete.", file=sys.stderr)
        sys.stderr.flush()
        sys.stdout.flush() 


if __name__ == "__main__":
    realtime_detection_for_java()