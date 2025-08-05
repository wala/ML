import tensorflow as tf


def add(a, b):
    return tf.add(a, b)


c = add(tf.eye(2, 3), tf.eye(2, 3))
