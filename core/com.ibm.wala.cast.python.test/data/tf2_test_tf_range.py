# From: https://www.tensorflow.org/versions/r2.9/api_docs/python/tf/range#for_example

import tensorflow as tf


def f(a):
    pass


start = 3
limit = 18
delta = 3

r = tf.range(start, limit, delta)

for i in r:
    f(i)
