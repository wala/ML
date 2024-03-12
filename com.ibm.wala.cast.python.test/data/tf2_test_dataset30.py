# From https://www.tensorflow.org/versions/r2.9/api_docs/python/tf/data/Dataset#concatenate.

import tensorflow as tf


def f(a):
    assert isinstance(a, tf.Tensor)


a = tf.data.Dataset.range(1, 4)  # ==> [ 1, 2, 3 ]
b = tf.data.Dataset.range(4, 8)  # ==> [ 4, 5, 6, 7 ]
ds = a.concatenate(b)

for element in ds:
    f(element)
