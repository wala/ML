import tensorflow as tf

def add(a, b):
  return a + b


c = add(tf.math.special.expint([1., 2.]), tf.math.special.expint([2., 2.]))
