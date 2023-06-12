import tensorflow as tf

def add(a, b):
  return a + b


c = add(tf.ones([1, 2]), tf.ones([2, 2]))  #  [[2., 2.], [2., 2.]]
