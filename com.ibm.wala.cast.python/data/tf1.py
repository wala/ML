from tensorflow.examples.tutorials.mnist import input_data
mnist = input_data.read_data_sets(False)

import tensorflow as tf

def inner_fn(a, b, c):
    return tf.reshape(a.data.images, [-1, 28, 28, 1])

def model_fn(a, b, c):
    x = inner_fn(a, b, c)
    return tf.conv2d(x, 32, 5, True)

def test_fn(a):
    return tf.reshape(a, 5)

model = tf.estimator.Estimator(model_fn)
    
# Define the input function for training
input_fn = tf.estimator.inputs.numpy_input_fn(
    {'images': mnist.test.images}, mnist.train.labels,
    5, -1, True)
# Train the Model
model.train(input_fn, 10)

test_fn(mnist.train.images)

