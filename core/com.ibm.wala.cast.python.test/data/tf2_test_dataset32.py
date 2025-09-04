# From https://www.tensorflow.org/versions/r2.9/api_docs/python/tf/data/Dataset#random.

import tensorflow as tf


def f(a):
    assert isinstance(a, tf.Tensor)


ds = tf.data.Dataset.random(seed=4).take(10)

for element in ds:
    f(element)
