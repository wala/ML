import tensorflow as tf
import pytest


@pytest.mark.skip
def f(a):
    assert isinstance(a, tf.Tensor)


f(tf.constant(1))
