from tensorflow.python.ops.random_ops import random_gamma
from tensorflow import random


def add(a, b):
    return a + b


c = add(random.gamma([10], [0.5, 1.5]), random_gamma([10], [1, 2.5]))
