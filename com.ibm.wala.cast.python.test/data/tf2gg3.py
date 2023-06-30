from tensorflow.python.ops.ragged.ragged_tensor import RaggedTensor

def add(a, b):
  return a + b


c = add(RaggedTensor.from_value_rowids([3, 1, 4, 1, 5, 9, 2, 6],[0, 0, 0, 0, 2, 2, 2, 3]), RaggedTensor.from_value_rowids([3, 1, 14, 1, 5, 19, 2, 16],[0, 0, 0, 0, 2, 2, 2, 3]))
