import tensorflow as tf
import pytest


def f(a):
    assert isinstance(a, tf.Tensor)


def test_dummy(x, test_input, expected):
    f(tf.constant(1))
