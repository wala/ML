from tensorflow.python.ops.ragged import ragged_factory_ops


def add(a, b):
    return a + b


c = add(ragged_factory_ops.constant([1, 2]), ragged_factory_ops.constant([2, 2]))
