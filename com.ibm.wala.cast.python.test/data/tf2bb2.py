import tensorflow


def add(a, b):
    return a + b


nested_value_rowids = [
    tensorflow.constant([0, 0, 1, 3, 3], tensorflow.int64),
    tensorflow.constant([0, 0, 2, 2, 2, 3, 4], tensorflow.int64),
]
x = tensorflow.keras.Input(shape=[None], dtype=tensorflow.string)
c = add(
    tensorflow.RaggedTensor.from_nested_value_rowids(x, nested_value_rowids),
    tensorflow.RaggedTensor.from_nested_value_rowids(x, nested_value_rowids),
)
