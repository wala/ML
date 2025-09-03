from tensorflow import *


def add(a, b):
    return a + b


c = add(ones([1, 2]), ones([2, 2]))
