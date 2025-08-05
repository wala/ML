import tensorflow as tf


def add(a, b):
    return a + b


c = add(tf.fill([1, 2], 2), tf.fill([2, 2], 1))
