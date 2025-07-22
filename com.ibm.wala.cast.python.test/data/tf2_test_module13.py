from tensorflow import Tensor


class C:

    def f(self, a):
        assert isinstance(a, Tensor)
