# From

import tensorflow as tf
from tensorflow.keras.preprocessing.image import ImageDataGenerator


@tf.function
def distributed_train_step(dataset_inputs):
    pass


EPOCHS = 40
IMG_SIZE = 112  # Input Image Size
BATCH_SIZE = 512  # Total 4 GPU, 128 batch per GPU

train_datagen = ImageDataGenerator(
    rescale=1.0 / 255, shear_range=0.2, zoom_range=0.2, horizontal_flip=False
)

train_generator = train_datagen.flow_from_directory(
    "./mnist/train",
    target_size=(IMG_SIZE, IMG_SIZE),
    batch_size=BATCH_SIZE,
    class_mode="categorical",
)

for epoch in range(1, EPOCHS + 1):
    batchs_per_epoch = len(train_generator)
    train_dataset = iter(train_generator)

    for _ in range(batchs_per_epoch):
        batch_loss = distributed_train_step(next(train_dataset))
