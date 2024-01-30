import tensorflow
from tensorflow.python.ops.math_ops import range


def add(a, b):
    return a + b


c = add(tensorflow.range(3, 18, 3), range(5))
