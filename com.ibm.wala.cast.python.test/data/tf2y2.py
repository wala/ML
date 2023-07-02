import tensorflow

def add(a, b):
  return a + b


c = add(tensorflow.ragged.range(3, 18, 3), tensorflow.ragged.range(6, 21, 3))
