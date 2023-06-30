from tensorflow.python.ops.random_ops import random_gamma
import tensorflow as tf

def add(a, b):
  return a + b


c = add(tf.random.gamma([10], [0.5, 1.5]), random_gamma([10], [1, 2.5]))
