import tensorflow as tf
import tensorflow


def add(a, b):
    return a + b


c = add(tf.random.uniform([1, 2]), tensorflow.random.uniform([2, 2]))
