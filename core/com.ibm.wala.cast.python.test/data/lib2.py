import numpy as np


class Lib(object):
    def es1(self, y, x):
        return x.foo() + y.foo()

    def es2(self, x):
        return np.arange(6).reshape(x)

    def es3(self, x):
        return x()
