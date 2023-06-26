import tensorflow as tf
from tensorflow import random

def add(a, b):
  return a + b


c = add(tf.random.uniform([1, 2]), random.uniform([2, 2]))
