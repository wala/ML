import tensorflow
from tensorflow.python.framework.constant_op import constant


def add(a, b):
    return a + b


c = add(tensorflow.constant([1, 2]), constant([2, 2]))
