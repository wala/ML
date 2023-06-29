import tensorflow

def add(a, b):
  return a + b


c = add(tensorflow.ones([1, 2]), tensorflow.ones([2, 2]))  #  [[2., 2.], [2., 2.]]
