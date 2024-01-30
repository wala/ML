import tensorflow


def add(a, b):
    return a + b


c = add(tensorflow.Variable([1.0, 2.0]), tensorflow.Variable([2.0, 2.0]))
