import cv2
import numpy as np
import tensorflow as tf
import sys
import base64

# Configuration
IMAGE_SIZE = (128, 128)
CLASS_NAMES = [chr(i) for i in range(ord('A'), ord('Z') + 1)]

def load_model():
    return tf.keras.models.load_model('../dataset/best_model.keras')

def process_frame(frame_bytes):
    try:
        img_bytes = base64.b64decode(frame_bytes)
        img_array = np.frombuffer(img_bytes, dtype=np.uint8)
        frame = cv2.imdecode(img_array, cv2.IMREAD_COLOR)

        if frame is None:
            raise ValueError("Failed to decode image")

        # Preprocessing
        frame = cv2.resize(frame, IMAGE_SIZE)
        frame = frame.astype(np.float32) / 255.0
        return np.expand_dims(frame, axis=0)
    except Exception as e:
        print(f"Decoding error: {str(e)}", file=sys.stderr, flush=True)
        return None

def main():
    model = load_model()
    print("Python ASL recognition ready", flush=True)

    while True:
        # Read from Java input
        frame_data = sys.stdin.readline().strip()
        if not frame_data:
            break

        try:
            # Process and predict
            processed = process_frame(frame_data)
            if processed is None:
                continue  # Skip to the next frame

            preds = model.predict(processed)
            pred_index = np.argmax(preds)
            confidence = preds[0][pred_index]

            # Send back to Java
            print(f"{CLASS_NAMES[pred_index]}:{confidence:.2f}", flush=True)

        except Exception as e:
            print(f"Error:{str(e)}", file=sys.stderr, flush=True)

if __name__ == "__main__":
    main()