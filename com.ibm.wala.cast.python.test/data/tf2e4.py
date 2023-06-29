import tensorflow as tensors
from tensorflow.python.ops.variables import Variable

def add(a, b):
  return a + b


c = add(tensors.Variable([1., 2.]), Variable([2., 2.]))
