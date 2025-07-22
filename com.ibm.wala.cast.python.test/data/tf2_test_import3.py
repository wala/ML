# Test https://github.com/wala/ML/issues/65.

from tensorflow import ones, Tensor


def g(a):
    assert isinstance(a, Tensor)


def f(a):
    assert isinstance(a, Tensor)
    g(ones([1, 2]))


f(ones([1, 2]))
