import tensorflow.python.ops.ragged.ragged_math_ops as tf

def add(a, b):
  return a + b


c = add(tf.range(3, 18, 3),tf.range(6, 21, 3))
