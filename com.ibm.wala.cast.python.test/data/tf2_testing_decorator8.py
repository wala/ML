import tensorflow as tf

@tf.function(experimental_relax_shapes=True)
def returned(a):
  return a

a = tf.range(5)
b = returned(a)
