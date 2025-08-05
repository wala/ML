import tensorflow as tf


def f(x):
    assert isinstance(x, tf.Tensor)


f(tf.constant(1))
