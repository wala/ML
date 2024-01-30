from tensorflow.python.ops.sparse_ops import sparse_eye
import tensorflow as tf


def add(a, b):
    return tf.sparse.add(a, b)


c = add(sparse_eye(2, 3), sparse_eye(2, 3))
