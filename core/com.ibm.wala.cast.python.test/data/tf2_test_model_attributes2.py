# From https://github.com/tensorflow/tensorflow/issues/14359#issue-272179775

import tensorflow as tf


def f(a):
    pass


inputs = tf.keras.Input(shape=(3,))
x = tf.keras.layers.Dense(4, activation=tf.nn.relu)(inputs)
outputs = tf.keras.layers.Dense(5, activation=tf.nn.softmax)(x)
model = tf.keras.Model(inputs=inputs, outputs=outputs)

for i in model.trainable_weights:
    f(i)
