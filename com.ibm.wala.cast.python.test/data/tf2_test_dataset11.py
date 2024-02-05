# Test enumerate. The first element of the tuple returned isn't a tensor.

import tensorflow as tf


def f(a):
    pass


def g(a):
    pass


dataset = tf.data.Dataset.from_tensor_slices([1, 2, 3])

for step, element in enumerate(dataset, 1):
    f(step)
    g(element)
