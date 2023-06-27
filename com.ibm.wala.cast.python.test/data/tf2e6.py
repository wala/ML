from tensorflow.python import ops

def add(a, b):
  return a + b


c = add(ops.variables.Variable([1., 2.]), ops.variables.Variable([2., 2.]))
