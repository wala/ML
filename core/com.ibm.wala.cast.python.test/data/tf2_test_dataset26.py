# From https://www.tensorflow.org/versions/r2.9/api_docs/python/tf/data/Dataset#enumerate.

import tensorflow as tf


def f(a):
    assert isinstance(a, tf.Tensor)


def g1(a):
    assert isinstance(a, tf.Tensor)


def g2(a):
    assert isinstance(a, tf.Tensor)


def g3(a):
    assert isinstance(a, tf.Tensor)


def h(a):
    assert isinstance(a, tuple)


dataset = tf.data.Dataset.from_tensor_slices([(7, 8), (9, 10)])
dataset = dataset.enumerate()

for element in dataset:
    assert isinstance(element, tuple)
    assert len(element) == 2
    f(element[0])
    g1(element[1])
    assert len(element[1] == 2)
    g2(element[1][0])
    g3(element[1][1])
    h(element)
