from tensorflow.python.ops.random_ops import random_uniform

def add(a, b):
  return a + b


c = add(random_uniform([1, 2]), random_uniform([2, 2]))
