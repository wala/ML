# From https://github.com/aymericdamien/TensorFlow-Examples/blob/6dcbe14649163814e72a22a999f20c5e247ce988/tensorflow_v2/notebooks/6_Hardware/multigpu_training.ipynb.
# %%
"""
# Multi-GPU Training Example

Train a convolutional neural network on multiple GPU with TensorFlow 2.0+.

- Author: Aymeric Damien
- Project: https://github.com/aymericdamien/TensorFlow-Examples/
"""

# %%
"""
## Training with multiple GPU cards

In this example, we are using data parallelism to split the training accross multiple GPUs. Each GPU has a full replica of the neural network model, and the weights (i.e. variables) are updated synchronously by waiting that each GPU process its batch of data.

First, each GPU process a distinct batch of data and compute the corresponding gradients, then, all gradients are accumulated in the CPU and averaged. The model weights are finally updated with the gradients averaged, and the new model weights are sent back to each GPU, to repeat the training process.

<img src="https://www.tensorflow.org/images/Parallelism.png" alt="Parallelism" style="width: 400px;"/>

## CIFAR10 Dataset Overview

The CIFAR-10 dataset consists of 60000 32x32 colour images in 10 classes, with 6000 images per class. There are 50000 training images and 10000 test images.

![CIFAR10 Dataset](https://storage.googleapis.com/kaggle-competitions/kaggle/3649/media/cifar-10.png)

More info: https://www.cs.toronto.edu/~kriz/cifar.html
"""

# %%

import tensorflow as tf
from tensorflow.keras import Model, layers
import time
import numpy as np

# %%
# MNIST dataset parameters.
num_classes = 10  # total classes (0-9 digits).
num_gpus = 4

# Training parameters.
learning_rate = 0.001
training_steps = 1000
# Split batch size equally between GPUs.
# Note: Reduce batch size if you encounter OOM Errors.
batch_size = 1024 * num_gpus
display_step = 20

# Network parameters.
conv1_filters = 64  # number of filters for 1st conv layer.
conv2_filters = 128  # number of filters for 2nd conv layer.
conv3_filters = 256  # number of filters for 2nd conv layer.
fc1_units = 2048  # number of neurons for 1st fully-connected layer.

# %%
# Prepare MNIST data.
from tensorflow.keras.datasets import cifar10
(x_train, y_train), (x_test, y_test) = cifar10.load_data()
# Convert to float32.
x_train, x_test = np.array(x_train, np.float32), np.array(x_test, np.float32)
# Normalize images value from [0, 255] to [0, 1].
x_train, x_test = x_train / 255., x_test / 255.
y_train, y_test = np.reshape(y_train, (-1)), np.reshape(y_test, (-1))

# %%
# Use tf.data API to shuffle and batch data.
train_data = tf.data.Dataset.from_tensor_slices((x_train, y_train))
train_data = train_data.repeat().shuffle(batch_size * 10).batch(batch_size).prefetch(num_gpus)


# %%
class ConvNet(Model):

    # Set layers.
    def __init__(self):
        super(ConvNet, self).__init__()

        # Convolution Layer with 64 filters and a kernel size of 3.
        self.conv1_1 = layers.Conv2D(conv1_filters, kernel_size=3, padding='SAME', activation=tf.nn.relu)
        self.conv1_2 = layers.Conv2D(conv1_filters, kernel_size=3, padding='SAME', activation=tf.nn.relu)
        # Max Pooling (down-sampling) with kernel size of 2 and strides of 2.
        self.maxpool1 = layers.MaxPool2D(2, strides=2)

        # Convolution Layer with 128 filters and a kernel size of 3.
        self.conv2_1 = layers.Conv2D(conv2_filters, kernel_size=3, padding='SAME', activation=tf.nn.relu)
        self.conv2_2 = layers.Conv2D(conv2_filters, kernel_size=3, padding='SAME', activation=tf.nn.relu)
        self.conv2_3 = layers.Conv2D(conv2_filters, kernel_size=3, padding='SAME', activation=tf.nn.relu)
        # Max Pooling (down-sampling) with kernel size of 2 and strides of 2.
        self.maxpool2 = layers.MaxPool2D(2, strides=2)

        # Convolution Layer with 256 filters and a kernel size of 3.
        self.conv3_1 = layers.Conv2D(conv3_filters, kernel_size=3, padding='SAME', activation=tf.nn.relu)
        self.conv3_2 = layers.Conv2D(conv3_filters, kernel_size=3, padding='SAME', activation=tf.nn.relu)
        self.conv3_3 = layers.Conv2D(conv3_filters, kernel_size=3, padding='SAME', activation=tf.nn.relu)

        # Flatten the data to a 1-D vector for the fully connected layer.
        self.flatten = layers.Flatten()

        # Fully connected layer.
        self.fc1 = layers.Dense(1024, activation=tf.nn.relu)
        # Apply Dropout (if is_training is False, dropout is not applied).
        self.dropout = layers.Dropout(rate=0.5)

        # Output layer, class prediction.
        self.out = layers.Dense(num_classes)

    # Set forward pass.
    @tf.function
    def call(self, x, is_training=False):
        x = self.conv1_1(x)
        x = self.conv1_2(x)
        x = self.maxpool1(x)
        x = self.conv2_1(x)
        x = self.conv2_2(x)
        x = self.conv2_3(x)
        x = self.maxpool2(x)
        x = self.conv3_1(x)
        x = self.conv3_2(x)
        x = self.conv3_3(x)
        x = self.flatten(x)
        x = self.fc1(x)
        x = self.dropout(x, training=is_training)
        x = self.out(x)
        if not is_training:
            # tf cross entropy expect logits without softmax, so only
            # apply softmax when not training.
            x = tf.nn.softmax(x)
        return x


# %%
# Cross-Entropy Loss.
# Note that this will apply 'softmax' to the logits.
@tf.function
def cross_entropy_loss(x, y):
    # Convert labels to int 64 for tf cross-entropy function.
    y = tf.cast(y, tf.int64)
    # Apply softmax to logits and compute cross-entropy.
    loss = tf.nn.sparse_softmax_cross_entropy_with_logits(labels=y, logits=x)
    # Average loss across the batch.
    return tf.reduce_mean(loss)


# Accuracy metric.
@tf.function
def accuracy(y_pred, y_true):
    # Predicted class is the index of highest score in prediction vector (i.e. argmax).
    correct_prediction = tf.equal(tf.argmax(y_pred, 1), tf.cast(y_true, tf.int64))
    return tf.reduce_mean(tf.cast(correct_prediction, tf.float32), axis=-1)


@tf.function
def backprop(batch_x, batch_y, trainable_variables):
    # Wrap computation inside a GradientTape for automatic differentiation.
    with tf.GradientTape() as g:
        # Forward pass.
        pred = conv_net(batch_x, is_training=True)
        # Compute loss.
        loss = cross_entropy_loss(pred, batch_y)
        # Compute gradients.
        gradients = g.gradient(loss, trainable_variables)
    return gradients


# Build the function to average the gradients.
@tf.function
def average_gradients(tower_grads):
    avg_grads = []
    for tgrads in zip(*tower_grads):
        grads = []
        for g in tgrads:
            expanded_g = tf.expand_dims(g, 0)
            grads.append(expanded_g)

        grad = tf.concat(axis=0, values=grads)
        grad = tf.reduce_mean(grad, 0)

        avg_grads.append(grad)

    return avg_grads


# %%
with tf.device('/cpu:0'):
    # Build convnet.
    conv_net = ConvNet()
    # Stochastic gradient descent optimizer.
    optimizer = tf.optimizers.Adam(learning_rate)


# %%
# Optimization process.
def run_optimization(x, y):
    # Save gradients for all GPUs.
    tower_grads = []
    # Variables to update, i.e. trainable variables.
    trainable_variables = conv_net.trainable_variables

    with tf.device('/cpu:0'):
        for i in range(num_gpus):
            # Split data between GPUs.
            gpu_batch_size = int(batch_size / num_gpus)
            batch_x = x[i * gpu_batch_size: (i + 1) * gpu_batch_size]
            batch_y = y[i * gpu_batch_size: (i + 1) * gpu_batch_size]

            # Build the neural net on each GPU.
            with tf.device('/gpu:%i' % i):
                grad = backprop(batch_x, batch_y, trainable_variables)
                tower_grads.append(grad)

                # Last GPU Average gradients from all GPUs.
                if i == num_gpus - 1:
                    gradients = average_gradients(tower_grads)

        # Update vars following gradients.
        optimizer.apply_gradients(list(zip(gradients, trainable_variables)))


# %%
# Run training for the given number of steps.
ts = time.time()
for step, (batch_x, batch_y) in enumerate(train_data.take(training_steps), 1):
    # Run the optimization to update W and b values.
    run_optimization(batch_x, batch_y)

    if step % display_step == 0 or step == 1:
        dt = time.time() - ts
        speed = batch_size * display_step / dt
        pred = conv_net(batch_x)
        loss = cross_entropy_loss(pred, batch_y)
        acc = accuracy(pred, batch_y)
        print(("step: %i, loss: %f, accuracy: %f, speed: %f examples/sec" % (step, loss, acc, speed)))
        ts = time.time()
