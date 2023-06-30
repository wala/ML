from tensorflow.python.ops.variables import Variable

def add(a, b):
  return a + b


c = add(Variable([1., 2.]), Variable([2., 2.]))
