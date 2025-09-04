import tensorflow as tf


@tf.function(autograph=False)
def returned(a):
    return a


a = tf.range(5)
b = returned(a)
