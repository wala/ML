import tensorflow as tf

def add(a, b):
  return a + b


c = add(tf.RaggedTensor.from_value_rowids([3, 1, 4, 1, 5, 9, 2, 6],[0, 0, 0, 0, 2, 2, 2, 3]), tf.RaggedTensor.from_value_rowids([3, 1, 14, 1, 5, 19, 2, 16],[0, 0, 0, 0, 2, 2, 2, 3]))
