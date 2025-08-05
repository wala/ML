import tensorflow as tf


@tf.function(jit_compile=True)
def returned(a):
    return a


a = tf.range(5)
b = returned(a)
