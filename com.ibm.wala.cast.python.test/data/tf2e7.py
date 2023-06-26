from tensorflow import python

def add(a, b):
  return a + b


c = add(python.ops.variables.Variable([1., 2.]), python.ops.variables.Variable([2., 2.]))
