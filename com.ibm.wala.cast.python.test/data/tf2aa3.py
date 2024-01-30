from tensorflow.python.ops.ragged.ragged_tensor import RaggedTensor


def add(a, b):
    return a + b


c = add(
    RaggedTensor.from_nested_row_splits(
        [3, 1, 4, 1, 5, 9, 2, 6], ([0, 3, 3, 5], [0, 4, 4, 7, 8, 8])
    ),
    RaggedTensor.from_nested_row_splits(
        [13, 1, 4, 1, 15, 9, 2, 16], ([0, 3, 3, 5], [0, 4, 4, 7, 8, 8])
    ),
)
