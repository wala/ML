import tensorflow as tf

@tf.function(reduce_retracing=True)
def returned(a):
  return a

a = tf.range(5)
b = returned(a)
