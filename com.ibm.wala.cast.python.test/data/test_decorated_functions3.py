import tensorflow as tf
import pytest


def f(a):
    assert isinstance(a, tf.Tensor)


@pytest.mark.parametrize("test_input,expected", [("3+5", 8), ("2+4", 6), ("6*9", 42)])
def test_dummy(x, test_input, expected):
    f(tf.constant(1))
