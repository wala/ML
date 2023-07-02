from tensorflow import linalg

def add(a, b):
  return a + b


c = add(linalg.eye(2), linalg.eye(2))
