from tensorflow.python.ops import variables


def add(a, b):
    return a + b


c = add(variables.Variable([1.0, 2.0]), variables.Variable([2.0, 2.0]))
