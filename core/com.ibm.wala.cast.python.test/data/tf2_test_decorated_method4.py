import tensorflow as tf


def mama(fun):
    return fun


def raffi(x):
    assert isinstance(x, tf.Tensor)


raffi(tf.constant(1))
