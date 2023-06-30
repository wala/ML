import tensorflow as tensors

def add(a, b):
  return a + b


c = add(tensors.ragged.range(3, 18, 3), tensors.ragged.range(6, 21, 3))
