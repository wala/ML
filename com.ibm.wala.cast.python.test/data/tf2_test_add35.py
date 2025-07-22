import tensorflow
from tensorflow.python.ops.array_ops import one_hot


def add(a, b):
    return a + b


c = add(tensorflow.one_hot([0, 1, 2], 3), one_hot([2, 4, 3], 3))
