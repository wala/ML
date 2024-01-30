#!/usr/bin/env python

import sys
import numpy as np
import tensorflow as tf
import tensorflow.keras as keras


class MyModel(keras.Model):

    def __init__(self):
        super(MyModel, self).__init__()
        self.conv1 = keras.layers.Conv2D(32, 3, activation="relu")
        self.flatten = keras.layers.Flatten()
        self.d1 = keras.layers.Dense(128, activation="relu")
        self.d2 = keras.layers.Dense(10, activation="softmax")

    def call(self, x):
        x = self.conv1(x)
        x = self.flatten(x)
        x = self.d1(x)
        x = self.d2(x)
        return x


@tf.function
def train_step(model, fn_loss, fn_accuracy, images, labels):
    with tf.GradientTape() as tape:
        predictions = model(images)
        loss = loss_object(labels, predictions)
    gradients = tape.gradient(loss, model.trainable_variables)
    optimizer.apply_gradients(zip(gradients, model.trainable_variables))

    fn_loss(loss)
    fn_accuracy(labels, predictions)


@tf.function
def test_step(model, fn_loss, fn_accuracy, images, labels):
    predictions = model(images)
    t_loss = loss_object(labels, predictions)

    fn_loss(t_loss)
    fn_accuracy(labels, predictions)


#
# Prepare training data
#
validation_split = 0.2

(x_train, y_train), (x_test, y_test) = keras.datasets.mnist.load_data()

x_train = x_train.astype(np.float32) / 255.0
x_test = x_test.astype(np.float32) / 255.0

num_train = int(x_train.shape[0] * (1.0 - validation_split))
x_valid = x_train[num_train:, ..., tf.newaxis]
y_valid = y_train[num_train:]
x_train = x_train[:num_train, ..., tf.newaxis]
y_train = y_train[:num_train]

x_test = x_test[..., tf.newaxis]

train_ds = tf.data.Dataset.from_tensor_slices((x_train, y_train)).shuffle(10000).batch(32)
valid_ds = tf.data.Dataset.from_tensor_slices((x_valid, y_valid)).batch(32)
test_ds = tf.data.Dataset.from_tensor_slices((x_test, y_test)).batch(32)

#
# Model and loss functions
#
model = MyModel()

loss_object = tf.keras.losses.SparseCategoricalCrossentropy()
optimizer = tf.keras.optimizers.Adam()

train_loss = tf.keras.metrics.Mean(name="train_loss")
train_accuracy = tf.keras.metrics.SparseCategoricalAccuracy(name="train_accuracy")

valid_loss = tf.keras.metrics.Mean(name="validation_loss")
valid_accuracy = tf.keras.metrics.SparseCategoricalAccuracy(name="validation_accuracy")

test_loss = tf.keras.metrics.Mean(name="test_loss")
test_accuracy = tf.keras.metrics.SparseCategoricalAccuracy(name="test_accuracy")

#
# Run training
#
EPOCHS = 10

min_loss = sys.float_info.max
for epoch in range(EPOCHS):
    for images, labels in train_ds:
        train_step(model, train_loss, train_accuracy, images, labels)

    for valid_images, valid_labels in valid_ds:
        test_step(model, valid_loss, valid_accuracy, valid_images, valid_labels)

    if valid_loss.result() < min_loss:
        min_loss = valid_loss.result()
        min_weights = model.get_weights()

    template = "Epoch {}, Loss: {:.4f}, Acc: {:.4f}, Val Loss: {:.4f}, Val Acc: {:.4f}, Min Loss: {:.4f}"
    print(template.format(epoch + 1,
                          train_loss.result(),
                          train_accuracy.result() * 100,
                          valid_loss.result(),
                          valid_accuracy.result() * 100,
                          min_loss))

    train_loss.reset_states()
    train_accuracy.reset_states()
    valid_loss.reset_states()
    valid_accuracy.reset_states()

model.set_weights(min_weights)

for test_images, test_labels in test_ds:
    test_step(model, test_loss, test_accuracy, test_images, test_labels)

print("Test Loss: {:.4f}, Test Accuracy: {:.4f}".format(test_loss.result(), test_accuracy.result()))

