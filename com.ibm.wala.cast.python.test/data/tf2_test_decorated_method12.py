# Test https://github.com/wala/ML/issues/188.

import tensorflow as tf


def mama(test=None):
    assert test == "Hello"

    def _mama(func):

        def core(*args, **kwargs):
            assert isinstance(args[0], tf.Tensor)
            return func(*args, **kwargs)

        return core

    return _mama


@mama(test="Hello")
def f(x):
    assert isinstance(x, tf.Tensor)
    return 5


res = f(tf.constant(1))
assert res == 5
