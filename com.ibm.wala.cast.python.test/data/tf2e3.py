import tensorflow
def add(a, b):
  return a + b


c = add(tensorflow.Variable([1., 2.]), tensorflow.Variable([2., 2.]))
