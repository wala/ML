import tensorflow as tf

def add(a, b):
  return a + b


c = add(tf.one_hot([0, 1, 2], 3), tf.one_hot([2,4,3], 3))
