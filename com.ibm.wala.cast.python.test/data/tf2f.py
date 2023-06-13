import tensorflow as tf

def add(a, b):
  return a + b


c = add(tf.constant([1, 2]), tf.constant([2, 2]))
