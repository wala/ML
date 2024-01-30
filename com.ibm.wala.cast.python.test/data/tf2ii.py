import tensorflow as tf


def add(a, b):
    return a + b


c = add(
    tf.random.normal([4], 0, 1, tf.float32), tf.random.normal([4], 2, 1, tf.float32)
)
