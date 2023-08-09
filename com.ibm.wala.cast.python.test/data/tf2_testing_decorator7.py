import tensorflow as tf

@tf.function(experimental_autograph_options=tf.autograph.experimental.Feature.EQUALITY_OPERATORS)
def returned(a):
  return a

a = tf.range(5)
b = returned(a)
