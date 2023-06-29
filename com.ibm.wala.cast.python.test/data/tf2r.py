from tensorflow import ones

def add(a, b):
  return a + b


c = add(ones([1, 2]), ones([2, 2]))  #  [[2., 2.], [2., 2.]]
