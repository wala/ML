# From https://www.tensorflow.org/versions/r2.9/api_docs/python/tf/data/Dataset#from_tensors.

import tensorflow as tf


def f(a):
    assert isinstance(a, tf.Tensor)


def g(a):
    assert isinstance(a, tf.Tensor)


def h(a):
    assert isinstance(a, tuple)


dataset = tf.data.Dataset.from_tensors(([1, 2, 3], "A"))

for element in dataset:
    f(element[0])
    g(element[1])
    h(element)
