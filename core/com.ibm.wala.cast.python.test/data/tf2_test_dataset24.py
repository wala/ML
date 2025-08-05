# From https://www.tensorflow.org/versions/r2.9/api_docs/python/tf/data/Dataset#reduce.

import tensorflow as tf
import numpy as np


def f(a):
    assert isinstance(a, tf.Tensor)


def g(a):
    assert isinstance(a, tf.Tensor)


element = tf.data.Dataset.range(5).reduce(np.int64(0), lambda x, _: x + 1)
assert isinstance(element, tf.Tensor)
f(element)

element = tf.data.Dataset.range(5).reduce(np.int64(0), lambda x, y: x + y)
assert isinstance(element, tf.Tensor)
g(element)
