from tensorflow.python.ops.ragged.ragged_factory_ops import constant
def add(a, b):
  return a + b


c = add(constant([1, 2]), constant([2, 2]))
