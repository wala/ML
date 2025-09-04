import tensorflow as tf


def mama(fun):
    return fun


@mama
def raffi(x):
    assert isinstance(x, tf.Tensor)


raffi(tf.constant(1))
