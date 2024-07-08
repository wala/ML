import tensorflow as tf


def add(a, b):
    return a + b


def gen_iter(ds):
    return iter(ds)


dataset = tf.data.Dataset.from_tensor_slices([1, 2, 3])

my_iter = gen_iter(dataset)
length = len(dataset)

for _ in range(length):
    element = next(my_iter)
    add(element, element)
