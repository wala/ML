import tensorflow
from tensorflow.python.framework.sparse_tensor import SparseTensor

def add(a, b):
  return tensorflow.sparse.add(a,b)


c = add(tensorflow.SparseTensor([[0, 0], [1, 2]], [1, 2],[3, 4]), SparseTensor([[0, 0], [1, 2]], [1, 2], [3, 4]))
