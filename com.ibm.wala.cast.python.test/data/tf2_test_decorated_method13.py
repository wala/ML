import tensorflow as tf


def mama(my_func=None):
    return my_func


@mama
def f(x):
    assert isinstance(x, tf.Tensor)


f(tf.constant(1))
