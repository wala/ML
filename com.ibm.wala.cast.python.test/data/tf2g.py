import tensorflow as tf

def add(a, b):
  return a + b


c = add(tf.zeros([1, 2]), tf.zeros([2, 2]))
