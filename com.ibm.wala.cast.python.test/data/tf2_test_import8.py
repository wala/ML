# Test https://github.com/wala/ML/issues/65.
# This is an invalid case. No wildcard import; we shouldn't present that there is one.

from tensorflow import Tensor


def g(a):
    assert isinstance(a, Tensor)


def f(a):
    g(ones([1, 2]))


f(5)
