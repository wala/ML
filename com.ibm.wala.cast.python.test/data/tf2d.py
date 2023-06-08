import tensorflow as tf

def add(a, b):
  return a + b


c = add(tf.random.uniform([1, 2]), tf.random.uniform([2, 2]))
