import tensorflow as tf


def add(a, b):
    return a + b


c = add(tf.ragged.range(3, 18, 3), tf.ragged.range(6, 21, 3))
