from tensorflow import ragged
def add(a, b):
  return a + b


c = add(ragged.constant([1, 2]), ragged.constant([2, 2]))
