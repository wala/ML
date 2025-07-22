import tensorflow as tf
from tensorflow.python.ops.variables import Variable


def add(a, b):
    return a + b


c = add(tf.Variable([1.0, 2.0]), Variable([2.0, 2.0]))
