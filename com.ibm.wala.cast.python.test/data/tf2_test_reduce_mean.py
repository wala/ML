# From https://www.tensorflow.org/versions/r2.9/api_docs/python/tf/math/reduce_mean#for_example
import tensorflow as tf


def f(a):
    pass


def g(a):
    pass


def h(a):
    pass


x = tf.constant([[1.0, 1.0], [2.0, 2.0]])
f(tf.reduce_mean(x))

g(tf.reduce_mean(x, 0))

h(tf.reduce_mean(x, 1))
