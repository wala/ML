import tensorflow as tf

def add(a, b):
  return a + b


c = add(tf.ragged.constant([1, 2]), tf.ragged.constant([2, 2]))
