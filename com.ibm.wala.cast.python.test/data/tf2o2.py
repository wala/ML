import tensorflow
from tensorflow.python.ops.linalg_ops import eye

def add(a, b):
  return tensorflow.add(a,b)


c = add(tensorflow.eye(2,3), eye(2,3))
