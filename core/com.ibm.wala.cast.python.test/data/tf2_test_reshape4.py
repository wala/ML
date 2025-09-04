# https://www.tensorflow.org/versions/r2.9/api_docs/python/tf/reshape

import tensorflow as tf


def f(a):
    pass


t1 = tf.ones([28, 28])
t2 = tf.reshape(t1, [-1, 28, 28, 1])
f(t2)
