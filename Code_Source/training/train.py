import os
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'
import pandas as pd
import numpy as np
import tensorflow as tf
from tensorflow.keras import layers, models, callbacks as kcallbacks
from sklearn.model_selection import train_test_split
from sklearn.utils.class_weight import compute_class_weight
import matplotlib.pyplot as plt


# Configuration
ROOT_DIR = os.getcwd()
BATCH_SIZE = 64
IMG_SIZE = (128, 128)
EPOCHS = 50
NUM_CLASSES = 29  

# Dataset Preparation
def generate_verified_csv():
    """Generate validated dataset CSV with quality checks"""
    dataset_path = os.path.join(ROOT_DIR, 'SigNN Character Database')
    csv_path = os.path.join(ROOT_DIR, 'train_verified.csv')
    
    data = []
    for char_dir in sorted(os.listdir(dataset_path)):
        if not char_dir.isalpha() or len(char_dir) != 1:
            continue
            
        char_path = os.path.join(dataset_path, char_dir)
        if not os.path.isdir(char_path):
            continue

        valid_images = []
        for img_file in os.listdir(char_path):
            file_path = os.path.join(char_path, img_file)
            try:
                img = tf.io.read_file(file_path)
                _ = tf.image.decode_image(img)
                if os.path.getsize(file_path) > 1024:
                    valid_images.append(img_file)
            except:
                print(f"Removing corrupted file: {file_path}")
        
        for img_file in valid_images:
            data.append({
                'path': os.path.join(char_path, img_file),
                'phrase': char_dir.upper()
            })

    df = pd.DataFrame(data)
    
    # Balance classes with minimum 50 samples
    min_samples = max(50, df['phrase'].value_counts().min())
    balanced_df = df.groupby('phrase').apply(
        lambda x: x.sample(min_samples, replace=True) if len(x) < min_samples else x.sample(min_samples)
    )
    
    balanced_df.to_csv(csv_path, index=False)
    print(f"Generated balanced dataset with {len(balanced_df)} entries")
    return balanced_df
print("startf")

# Load dataset
csv_path = os.path.join(ROOT_DIR, 'train_verified.csv')
dataset_df = generate_verified_csv() if not os.path.exists(csv_path) else pd.read_csv(csv_path)

# Optimized Data Pipeline
class ASLDataGenerator(tf.keras.utils.Sequence):
    def __init__(self, df, batch_size=BATCH_SIZE, augment=True):
        self.df = df
        self.batch_size = batch_size
        self.augment = augment
        self.label_encoder = layers.StringLookup(
            num_oov_indices=0, vocabulary=sorted(df['phrase'].unique()))
        
        # Moderate augmentation
        self.augmentation = tf.keras.Sequential([
            layers.RandomFlip("horizontal"),
            layers.RandomRotation(0.1),
            layers.RandomZoom(0.1),
            layers.RandomContrast(0.1)
        ])
        
        self.image_paths = df['path'].values
        self.labels = self.label_encoder(df['phrase']).numpy()
        
    def __len__(self):
        return int(np.ceil(len(self.df) / self.batch_size))
    
    def __getitem__(self, index):
        batch_paths = self.image_paths[index*self.batch_size:(index+1)*self.batch_size]
        batch_labels = self.labels[index*self.batch_size:(index+1)*self.batch_size]
        
        images = []
        for path in batch_paths:
            img = tf.io.read_file(path)
            img = tf.image.decode_jpeg(img, channels=3)
            img = tf.image.resize(img, IMG_SIZE)
            img = tf.cast(img, tf.float32) / 255.0
            
            if self.augment:
                img = self.augmentation(img)
                
            images.append(img)
            
        return tf.stack(images), tf.stack(batch_labels)
    
    def on_epoch_end(self):
        indices = np.arange(len(self.df))
        np.random.shuffle(indices)
        self.image_paths = self.image_paths[indices]
        self.labels = self.labels[indices]

# Model Architecture
def build_optimized_model():
    base_model = tf.keras.applications.MobileNetV2(
        input_shape=(*IMG_SIZE, 3),
        include_top=False,
        weights='imagenet'
    )
    
    # Freeze initial layers
    for layer in base_model.layers[:50]:
        layer.trainable = False
        
    model = models.Sequential([
        layers.Input(shape=(*IMG_SIZE, 3)),
        base_model,
        layers.GlobalAveragePooling2D(),
        layers.Dense(128, activation='relu'),
        layers.Dropout(0.3),
        layers.Dense(NUM_CLASSES, activation='softmax')
    ])
    
    model.compile(
        optimizer=tf.keras.optimizers.SGD(
            learning_rate=0.01,
            momentum=0.9,
            nesterov=True
        ),
        loss='sparse_categorical_crossentropy',
        metrics=['accuracy']
    )
    return model

# Training Workflow
def main():
    print("\nSample data verification:")
    sample = dataset_df.sample(5)
    for idx, row in sample.iterrows():
        print(f"Path: {row['path']} | Label: {row['phrase']} | Exists: {os.path.exists(row['path'])}")
    
    # Split dataset
    train_df, val_df = train_test_split(
        dataset_df,
        test_size=0.2,
        stratify=dataset_df['phrase'],
        random_state=42
    )
    
    # Create generators
    train_gen = ASLDataGenerator(train_df, augment=True)
    val_gen = ASLDataGenerator(val_df, augment=False)
    
    # Class weighting
    class_weights = compute_class_weight(
        'balanced',
        classes=np.unique(dataset_df['phrase']),
        y=dataset_df['phrase']
    )
    class_weight_dict = dict(enumerate(class_weights))
    
    model_callbacks = [
        kcallbacks.ModelCheckpoint(
            os.path.join(ROOT_DIR, 'best_model.keras'),
            monitor='val_accuracy',
            save_best_only=True,
            mode='max'
        ),
        kcallbacks.EarlyStopping(
            monitor='val_accuracy',
            patience=10,
            baseline=0.3,
            restore_best_weights=True
        ),
        kcallbacks.LearningRateScheduler(
            lambda epoch, lr: lr * 0.9 if epoch > 10 else lr
        )
    ]
    
    # Initialize model
    model = build_optimized_model()
    model.summary()
    
    print("\nStarting training...")
    history = model.fit(
        train_gen,
        validation_data=val_gen,
        epochs=EPOCHS,
        callbacks=model_callbacks,
        class_weight=class_weight_dict,
        verbose=1
    )
    
    # Evaluation
    model = models.load_model(os.path.join(ROOT_DIR, 'best_model.keras'))
    test_loss, test_acc = model.evaluate(val_gen)
    print(f"\nFinal Test Accuracy: {test_acc:.2%}")
    
    # Training curves
    plt.figure(figsize=(12, 5))
    plt.subplot(1, 2, 1)
    plt.plot(history.history['accuracy'], label='Train')
    plt.plot(history.history['val_accuracy'], label='Validation')
    plt.title('Accuracy')
    plt.legend()
    
    plt.subplot(1, 2, 2)
    plt.plot(history.history['loss'], label='Train')
    plt.plot(history.history['val_loss'], label='Validation')
    plt.title('Loss')
    plt.legend()
    plt.show()

if __name__ == "__main__":
    main()