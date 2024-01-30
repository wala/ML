from tensorflow import random
from tensorflow.python.ops.random_ops import random_poisson_v2


def add(a, b):
    return a + b


c = add(random.poisson([10], [0.5, 1.5]), random_poisson_v2([10], [1, 2.5]))
