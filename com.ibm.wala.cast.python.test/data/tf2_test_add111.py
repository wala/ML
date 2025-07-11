import tensorflow as tf


def add(a, b):
    return tf.sparse.add(a, b)


c = add(tf.sparse.eye(2, 3), tf.sparse.eye(2, 3))
