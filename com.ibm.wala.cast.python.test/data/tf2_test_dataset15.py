# Test enumerate. The first element of the tuple returned isn't a tensor.

import tensorflow as tf


def f(a):
    pass


def g(a):
    pass


def h(eds):
    for step, element in eds:
        f(step)
        g(element)


dataset = tf.data.Dataset.from_tensor_slices([1, 2, 3])
h(enumerate(dataset, 1))
