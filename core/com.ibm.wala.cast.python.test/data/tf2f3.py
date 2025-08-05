import tensorflow as tf
from tensorflow.python.framework.constant_op import constant


def add(a, b):
    return a + b


c = add(tf.constant([1, 2]), constant([2, 2]))
