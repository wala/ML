import tensorflow as tf

def add(a, b):
  return a + b


c = add(tf.range(3, 18, 3), tf.range(5))
