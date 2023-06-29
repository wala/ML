import tensorflow
from tensorflow.python.ops.array_ops import zeros_like

def add(a, b):
  return a + b


c = add(tensorflow.zeros_like([1, 2]), zeros_like([2, 2]))
