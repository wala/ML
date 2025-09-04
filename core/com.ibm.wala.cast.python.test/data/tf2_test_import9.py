# Test https://github.com/wala/ML/issues/65.

from tensorflow import *


def g(a):
    assert isinstance(a, Tensor)


def f(a):
    assert isinstance(a, Tensor)
    g(a)


f(ones([1, 2]))
