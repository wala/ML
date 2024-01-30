import tensorflow as tensor


def add(a, b):
    return a + b


c = add(tensor.linalg.eye(2), tensor.linalg.eye(2))
