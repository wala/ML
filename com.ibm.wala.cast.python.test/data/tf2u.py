import tensorflow as tf

def add(a, b):
  return a + b


c = add(tf.linalg.eye(2), tf.linalg.eye(2))
