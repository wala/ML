from tensorflow import RaggedTensor

def add(a, b):
  return a + b


c = add(RaggedTensor.from_row_lengths([3, 1, 4, 1, 5, 9, 2, 6], [4, 0, 3, 1, 0]), RaggedTensor.from_row_lengths([3, 11, 4, 11, 5, 19, 21, 6], [4, 0, 3, 1, 0]))
