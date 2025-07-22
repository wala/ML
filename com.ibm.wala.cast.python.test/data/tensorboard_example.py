# %%
"""
## Tensorboard
Graph, Loss, Accuracy & Weights visualization using Tensorboard and TensorFlow v2. This example is using the MNIST database of handwritten digits (http://yann.lecun.com/exdb/mnist/).

- Author: Aymeric Damien
- Project: https://github.com/aymericdamien/TensorFlow-Examples/
"""

# %%

import tensorflow as tf
import numpy as np

from scripts.utils import write_csv
import timeit

# %%
# Path to save logs into.
logs_path = "/tmp/tensorflow_logs/example/"

# MNIST dataset parameters.
num_classes = 10  # total classes (0-9 digits).
num_features = 784  # data features (img shape: 28*28).

# Training parameters.
learning_rate = 0.001
training_steps = 3000
batch_size = 256
display_step = 100

# Network parameters.
n_hidden_1 = 128  # 1st layer number of neurons.
n_hidden_2 = 256  # 2nd layer number of neurons.

# %%
# Prepare MNIST data.
from tensorflow.keras.datasets import mnist

(x_train, y_train), (x_test, y_test) = mnist.load_data()
# Convert to float32.
x_train, x_test = np.array(x_train, np.float32), np.array(x_test, np.float32)
# Flatten images to 1-D vector of 784 features (28*28).
x_train, x_test = x_train.reshape([-1, num_features]), x_test.reshape(
    [-1, num_features]
)
# Normalize images value from [0, 255] to [0, 1].
x_train, x_test = x_train / 255.0, x_test / 255.0

# %%
# Use tf.data API to shuffle and batch data.
train_data = tf.data.Dataset.from_tensor_slices((x_train, y_train))
train_data = train_data.repeat().shuffle(5000).batch(batch_size).prefetch(1)

start_time = timeit.default_timer()
skipped_time = 0

# %%
# Store layers weight & bias

# A random value generator to initialize weights.
random_normal = tf.initializers.RandomNormal()

weights = {
    "h1_weights": tf.Variable(
        random_normal([num_features, n_hidden_1]), name="h1_weights"
    ),
    "h2_weights": tf.Variable(
        random_normal([n_hidden_1, n_hidden_2]), name="h2_weights"
    ),
    "logits_weights": tf.Variable(
        random_normal([n_hidden_2, num_classes]), name="logits_weights"
    ),
}
biases = {
    "h1_bias": tf.Variable(tf.zeros([n_hidden_1]), name="h1_bias"),
    "h2_bias": tf.Variable(tf.zeros([n_hidden_2]), name="h2_bias"),
    "logits_bias": tf.Variable(tf.zeros([num_classes]), name="logits_bias"),
}

# %%
# Construct model and encapsulating all ops into scopes, making
# Tensorboard's Graph visualization more convenient.


# The computation graph to be traced.
@tf.function
def neural_net(x):
    with tf.name_scope("Model"):
        with tf.name_scope("HiddenLayer1"):
            # Hidden fully connected layer with 128 neurons.
            layer_1 = tf.add(tf.matmul(x, weights["h1_weights"]), biases["h1_bias"])
            # Apply sigmoid to layer_1 output for non-linearity.
            layer_1 = tf.nn.sigmoid(layer_1)
        with tf.name_scope("HiddenLayer2"):
            # Hidden fully connected layer with 256 neurons.
            layer_2 = tf.add(
                tf.matmul(layer_1, weights["h2_weights"]), biases["h2_bias"]
            )
            # Apply sigmoid to layer_2 output for non-linearity.
            layer_2 = tf.nn.sigmoid(layer_2)
        with tf.name_scope("LogitsLayer"):
            # Output fully connected layer with a neuron for each class.
            out_layer = (
                tf.matmul(layer_2, weights["logits_weights"]) + biases["logits_bias"]
            )
            # Apply softmax to normalize the logits to a probability distribution.
            out_layer = tf.nn.softmax(out_layer)
    return out_layer


# %%
# Cross-Entropy loss function.
def cross_entropy(y_pred, y_true):
    with tf.name_scope("CrossEntropyLoss"):
        # Encode label to a one hot vector.
        y_true = tf.one_hot(y_true, depth=num_classes)
        # Clip prediction values to avoid log(0) error.
        y_pred = tf.clip_by_value(y_pred, 1e-9, 1.0)
        # Compute cross-entropy.
        return tf.reduce_mean(-tf.reduce_sum(y_true * tf.math.log(y_pred)))


# Accuracy metric.
def accuracy(y_pred, y_true):
    with tf.name_scope("Accuracy"):
        # Predicted class is the index of highest score in prediction vector (i.e. argmax).
        correct_prediction = tf.equal(tf.argmax(y_pred, 1), tf.cast(y_true, tf.int64))
        return tf.reduce_mean(tf.cast(correct_prediction, tf.float32), axis=-1)


# Stochastic gradient descent optimizer.
with tf.name_scope("Optimizer"):
    optimizer = tf.optimizers.SGD(learning_rate)


# %%
# Optimization process.
def run_optimization(x, y):
    # Wrap computation inside a GradientTape for automatic differentiation.
    with tf.GradientTape() as g:
        pred = neural_net(x)
        loss = cross_entropy(pred, y)

    # Variables to update, i.e. trainable variables.
    trainable_variables = list(weights.values()) + list(biases.values())

    # Compute gradients.
    gradients = g.gradient(loss, trainable_variables)

    # Update weights/biases following gradients.
    optimizer.apply_gradients(list(zip(gradients, trainable_variables)))


# %%
# Visualize weights & biases as histogram in Tensorboard.
def summarize_weights(step):
    for w in weights:
        tf.summary.histogram(w.replace("_", "/"), weights[w], step=step)
    for b in biases:
        tf.summary.histogram(b.replace("_", "/"), biases[b], step=step)


# %%
# Create a Summary Writer to log the metrics to Tensorboad.
summary_writer = tf.summary.create_file_writer(logs_path)

total_loss = 0
loss_count = 0

total_accuracy = 0
accuracy_count = 0

# %%
# Run training for the given number of steps.
for step, (batch_x, batch_y) in enumerate(train_data.take(training_steps), 1):

    # Start to trace the computation graph. The computation graph remains
    # the same at each step, so we just need to export it once.
    if step == 1:
        tf.summary.trace_on(graph=True, profiler=True)

    # Run the optimization (computation graph).
    run_optimization(batch_x, batch_y)

    # Export the computation graph to tensorboard after the first
    # computation step was performed.
    if step == 1:
        with summary_writer.as_default():
            tf.summary.trace_export(name="trace", step=0, profiler_outdir=logs_path)

    if step % display_step == 0:
        pred = neural_net(batch_x)
        loss = cross_entropy(pred, batch_y)
        total_loss += loss
        loss_count += 1
        acc = accuracy(pred, batch_y)
        total_accuracy += acc
        accuracy_count += 1
        print_time = timeit.default_timer()
        print("step: %i, loss: %f, accuracy: %f" % (step, loss, acc))
        skipped_time += timeit.default_timer() - print_time

        # Write loss/acc metrics & weights to Tensorboard every few steps,
        # to avoid storing too much data.
        with summary_writer.as_default():
            tf.summary.scalar("loss", loss, step=step)
            tf.summary.scalar("accuracy", acc, step=step)
            summarize_weights(step)

time = timeit.default_timer() - start_time - skipped_time
avg_loss = float(total_loss) / float(loss_count)
avg_accuracy = float(total_accuracy) / float(accuracy_count)

write_csv(__file__, training_steps, float(avg_accuracy), float(avg_loss), time)

# %%
"""
### Run Tensorboard

To run tensorboard, run the following command in your terminal:
```
tensorboard --logdir=/tmp/tensorflow_logs
```

And then connect your web browser to: [http://localhost:6006](http://localhost:6006)

"""

# %%
"""
![tensorboard1](../../../resources/img/tf2/tensorboard1.png)
"""

# %%
"""
![tensorboard2](../../../resources/img/tf2/tensorboard2.png)
"""

# %%
"""
![tensorboard3](../../../resources/img/tf2/tensorboard3.png)
"""

# %%
"""
![tensorboard4](../../../resources/img/tf2/tensorboard4.png)
"""
