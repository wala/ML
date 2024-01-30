from tensorflow import random
from tensorflow.python.ops.random_ops import truncated_normal


def add(a, b):
    return a + b


c = add(random.truncated_normal([2]), truncated_normal([2], 3, 1))
