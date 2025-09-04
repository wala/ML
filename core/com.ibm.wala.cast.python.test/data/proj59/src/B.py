# Test https://github.com/wala/ML/issues/163.

from tensorflow import Tensor


class C:

    def __init__(self):
        pass

    def __call__(self, a):
        assert isinstance(a, Tensor)
