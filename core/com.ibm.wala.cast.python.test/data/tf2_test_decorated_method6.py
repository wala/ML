import tensorflow as tf


def mama(fun):

    def wrapper_fun(*args, **kwargs):
        assert isinstance(args[0], tf.Tensor)
        fun(*args, **kwargs)

    return wrapper_fun


def f(x):
    assert isinstance(x, tf.Tensor)


f(tf.constant(1))
