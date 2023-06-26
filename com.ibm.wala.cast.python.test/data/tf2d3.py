from tensorflow import random
from tensorflow.random import uniform

def add(a, b):
  return a + b


c = add(uniform([1, 2]), random.uniform([2, 2]))
