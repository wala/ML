# Test https://github.com/wala/ML/issues/163.

from tensorflow import Tensor
from src.tf2_test_module11a import C


class D(C):

    def g(self, a):
        assert isinstance(a, Tensor)
