# From https://github.com/tensorflow/tensorflow/issues/14359#issue-272179775

import tensorflow as tf


def f(a):
    pass


a = tf.keras.layers.Input(shape=(64,))
b = tf.keras.layers.Dense(5)(a)
model = tf.keras.models.Model(a, b)

for i in model.trainable_weights:
    f(i)
