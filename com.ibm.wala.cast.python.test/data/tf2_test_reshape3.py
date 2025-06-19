# https://www.tensorflow.org/versions/r2.9/api_docs/python/tf/reshape

import tensorflow as tf


def f(a):
    pass


t1 = [[1, 2, 3], [4, 5, 6]]
t2 = tf.reshape(t1, [6])
f(t2)
