import tensorflow as tf
from tensorflow.python.ops.ragged.ragged_tensor import RaggedTensor

def add(a, b):
  return a + b


c = add(tf.RaggedTensor.from_row_starts([3, 1, 4, 1, 5, 9, 2, 6], [0, 4, 4, 7, 8]), RaggedTensor.from_row_starts([3, 11, 4, 11, 5, 19, 21, 6], [0, 4, 4, 7, 8]))
