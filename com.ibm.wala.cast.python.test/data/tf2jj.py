import tensorflow as tf


def add(a, b):
    return a + b


c = add(tf.random.poisson([10], [0.5, 1.5]), tf.random.poisson([10], [1, 2.5]))
