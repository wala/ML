# From https://www.tensorflow.org/versions/r2.9/api_docs/python/tf/math/multiply#for_example/

import tensorflow as tf

def f(a):
    pass

x = tf.constant(([1, 2, 3, 4]))
f(tf.math.multiply(x, x))
