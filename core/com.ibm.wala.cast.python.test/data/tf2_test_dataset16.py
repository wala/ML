import tensorflow as tf


def add(a, b):
    return a + b


dataset = tf.data.Dataset.from_tensor_slices([1, 2, 3])

my_iter = iter(dataset)
length = len(dataset)

for _ in range(length):
    element = next(my_iter)
    add(element, element)
