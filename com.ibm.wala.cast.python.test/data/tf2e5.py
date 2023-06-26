from tensorflow.python.ops import variables

def add(a, b):
  return a + b


c = add(variables.Variable([1., 2.]), variables.Variable([2., 2.]))
