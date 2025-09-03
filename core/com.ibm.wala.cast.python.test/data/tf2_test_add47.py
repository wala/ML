from tensorflow import SparseTensor


def add(a, b):
    return a


c = add(
    SparseTensor([[0, 0], [1, 2]], [1, 2], [3, 4]),
    SparseTensor([[0, 0], [1, 2]], [1, 2], [3, 4]),
)
