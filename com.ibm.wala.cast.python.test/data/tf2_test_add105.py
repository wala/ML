import tensorflow as tensors


def add(a, b):
    return a + b


c = add(
    tensors.random.normal([4], 0, 1, tensors.float32),
    tensors.random.normal([4], 2, 1, tensors.float32),
)
