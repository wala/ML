import tensorflow as tf


def add(a, b):
    return a + b


c = add(tf.zeros_like([1, 2]), tf.zeros_like([2, 2]))
