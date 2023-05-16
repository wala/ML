import tensorflow as tf
from tensorflow.python.ops.array_ops import ones

def add(a, b):
  return a + b


c = add(tf.ones([1, 2]), ones([2, 2]))  #  [[2., 2.], [2., 2.]]
