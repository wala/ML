from tensorflow.examples.tutorials.mnist import input_data
mnist = input_data.read_data_sets(one_hot=False)

import tensorflow as tf

def model_fn(a, b, c):
    return 0

model = tf.estimator.Estimator(model_fn)
    
# Define the input function for training
input_fn = tf.estimator.inputs.numpy_input_fn(
    x={'images': mnist.train.images}, y=mnist.train.labels,
    batch_size=5, num_epochs=-1, shuffle=True)
# Train the Model
model.train(input_fn, steps=10)

