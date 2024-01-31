# From https://www.tensorflow.org/versions/r2.9/api_docs/python/tf/GradientTape#gradient.

import tensorflow as tf


def f(a):
    pass


x = tf.ragged.constant([[1.0, 2.0], [3.0]])
with tf.GradientTape() as g:
    g.watch(x)
    y = tf.multiply(x, x)
f(g.gradient(y, x))
