# Test https://github.com/wala/ML/issues/163.

from tensorflow import ones
from src import C


class D:

    def __init__(self):
        C().f(ones([1, 2]))


D()
