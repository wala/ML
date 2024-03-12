# From https://www.tensorflow.org/versions/r2.9/api_docs/python/tf/data/Dataset#from_tensors.

import tensorflow as tf


def f(a):
    assert isinstance(a, tf.Tensor)


def g(a):
    assert isinstance(a, tf.Tensor)


def h(a):
    assert isinstance(a, tf.Tensor)


def i(a):
    assert isinstance(a, tf.Tensor)


dataset = tf.data.Dataset.from_tensors([1, 2, 3])

for element in dataset:
    f(element)
    g(element[0])
    h(element[1])
    i(element[2])
