# Test https://github.com/wala/ML/issues/211.

import tensorflow as tf


def f(a):
    assert isinstance(a, tf.Tensor)
