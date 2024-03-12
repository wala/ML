# From https://www.tensorflow.org/versions/r2.9/api_docs/python/tf/data/Dataset#enumerate.

import tensorflow as tf


def f(a):
    assert isinstance(a, tf.Tensor)


def g(a):
    assert isinstance(a, tf.Tensor)


def h(a):
    assert isinstance(a, tuple)


dataset = tf.data.Dataset.from_tensor_slices([1, 2, 3])
dataset = dataset.enumerate(start=5)

for element in dataset:
    assert isinstance(element, tuple)
    assert len(element) == 2
    f(element[0])
    g(element[1])
    h(element)
