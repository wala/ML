import tensorflow as tf


class C:

    def __init__(self, some_iter):
        self.some_iter = some_iter

    def __str__(self):
        return str(self.some_iter)


def add(a, b):
    return a + b


def gen_iter(dataset):
    my_iter = iter(dataset)
    return C(my_iter)


dataset = tf.data.Dataset.from_tensor_slices([1, 2, 3])
c = gen_iter(dataset)
length = len(dataset)

for _ in range(length):
    element = next(c.some_iter)
    add(element, element)
