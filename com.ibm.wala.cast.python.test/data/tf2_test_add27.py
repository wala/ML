import tensorflow
from tensorflow.python.ops.array_ops import zeros


def add(a, b):
    return a + b


c = add(tensorflow.zeros([1, 2]), zeros([2, 2]))
