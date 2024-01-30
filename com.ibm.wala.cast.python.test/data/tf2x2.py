import tensorflow


def add(a, b):
    return tensorflow.sparse.add(a, b)


c = add(
    tensorflow.sparse.SparseTensor([[0, 0], [1, 2]], [1, 2], [3, 4]),
    tensorflow.sparse.SparseTensor([[0, 0], [1, 2]], [1, 2], [3, 4]),
)
