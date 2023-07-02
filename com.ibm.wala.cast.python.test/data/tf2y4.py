from tensorflow.python.ops.ragged.ragged_math_ops import range

def add(a, b):
  return a + b


c = add(range(3, 18, 3), range(6, 21, 3))
