import tensorflow as tf

def add(a, b):
  return a + b


nested_value_rowids = [tf.constant([0, 0, 1, 3, 3], tf.int64),tf.constant([0, 0, 2, 2, 2, 3, 4], tf.int64)]
x = tf.keras.Input(shape=[None], dtype=tf.string)
c = add(tf.RaggedTensor.from_nested_value_rowids(x, nested_value_rowids),tf.RaggedTensor.from_nested_value_rowids(x, nested_value_rowids))
