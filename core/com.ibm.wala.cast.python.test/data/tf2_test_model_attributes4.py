# From https://github.com/tensorflow/tensorflow/issues/14359#issue-272179775

import tensorflow as tf


def f(a):
    pass


a = tf.keras.layers.Input(shape=(64,))
b = tf.keras.layers.Dense(5)(a)
model = tf.keras.models.Model(a, b)

# From https://www.tensorflow.org/guide/keras/transfer_learning#freezing_layers_understanding_the_trainable_attribute.
for i in model.weights:
    f(i)
