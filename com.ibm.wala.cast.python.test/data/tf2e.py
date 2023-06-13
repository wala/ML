import tensorflow as tf

def add(a, b):
  return a + b


c = add(tf.Variable([1., 2.]), tf.Variable([2., 2.]))
