from tensorflow import experimental
import tensorflow as tf

def value_index(a,b):
  return a.value_index + b.value_index

# From https://www.tensorflow.org/versions/r2.9/api_docs/python/tf/Graph#using_graphs_directly_deprecated
g = tf.Graph()
with g.as_default():
  # Defines operation and tensor in graph
  c = tf.constant(30.0)
  assert c.graph is g

result = value_index(experimental.numpy.ndarray(g.get_operations()[0], 0, tf.float32), experimental.numpy.ndarray(g.get_operations()[0], 0, tf.float32))
