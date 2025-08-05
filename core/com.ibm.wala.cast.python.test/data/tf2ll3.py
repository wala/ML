from tensorflow.python.ops.sparse_ops import sparse_eye
import tensorflow as tf
from tensorflow import sparse


def add(a, b):
    return tf.sparse.add(a, b)


c = add(sparse.eye(2, 3), sparse_eye(2, 3))
