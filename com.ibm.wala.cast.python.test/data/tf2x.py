import tensorflow as tf

def add(a, b):
  return tf.sparse.add(a,b)


c = add(tf.sparse.SparseTensor([[0, 0], [1, 2]], [1, 2],[3, 4]), tf.sparse.SparseTensor([[0, 0], [1, 2]], [1, 2], [3, 4]))
