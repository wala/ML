import tensorflow
from tensorflow.python.ops.array_ops import fill


def add(a, b):
    return a + b


c = add(tensorflow.fill([1, 2], 2), fill([2, 2], 1))
