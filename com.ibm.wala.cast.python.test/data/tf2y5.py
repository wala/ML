from tensorflow.python.ops.ragged import ragged_math_ops


def add(a, b):
    return a + b


c = add(ragged_math_ops.range(3, 18, 3), ragged_math_ops.range(6, 21, 3))
