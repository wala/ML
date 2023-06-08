import tensorflow as tf

# From https://www.tensorflow.org/guide/function#autograph_transformations.
# A simple loop


# @tf.function
def f(x):
  while tf.reduce_sum(x) > 1:
    tf.print(x)
    x = tf.tanh(x)
  return x


f(tf.random.uniform([5]))
