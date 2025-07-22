from tensorflow import sparse
import tensorflow as tf


def add(a, b):
    return tf.sparse.add(a, b)


c = add(
    sparse.SparseTensor([[0, 0], [1, 2]], [1, 2], [3, 4]),
    sparse.SparseTensor([[0, 0], [1, 2]], [1, 2], [3, 4]),
)
