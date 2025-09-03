import tensorflow as tf


@tf.function()
def returned(a):
    return a


a = tf.range(5)
b = returned(a)
