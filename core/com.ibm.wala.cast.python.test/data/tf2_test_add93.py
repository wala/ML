import tensorflow as tf


def add(a, b):
    return a + b


c = add(
    tf.RaggedTensor.from_row_splits([3, 1, 4, 1, 5, 9, 2, 6], [0, 4, 4, 7, 8, 8]),
    tf.RaggedTensor.from_row_splits([2, 3, 7, 17, 8, 19, 2, 6], [0, 4, 4, 7, 8, 8]),
)
