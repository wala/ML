import tensorflow as tf


def add(a, b):
    return a + b


c = add(tf.random.truncated_normal([2]), tf.random.truncated_normal([2], 3, 1))
