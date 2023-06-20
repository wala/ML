import tensorflow as tf

def add(a, b):
  return a + b


c = add(tf.convert_to_tensor(1), tf.convert_to_tensor(2))
