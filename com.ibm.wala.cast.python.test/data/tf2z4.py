from tensorflow.python.ops.ragged.ragged_tensor import RaggedTensor
import tensorflow as tf

def add(a, b):
  return a + b

nested_row_lengths = [tf.constant([2, 1, 0, 2], tf.int64),tf.constant([2, 0, 3, 1, 1], tf.int64),]
x = tf.keras.Input(shape=[None], dtype=tf.string)
y = add(RaggedTensor.from_nested_row_lengths(x, nested_row_lengths),RaggedTensor.from_nested_row_lengths(x, nested_row_lengths))
