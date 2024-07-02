import tensorflow as tf


class C:

    def __init__(self, some_iter):
        self.some_iter = some_iter

    def __str__(self):
        return str(self.some_iter)


def add(a, b):
    return a + b


dataset = tf.data.Dataset.from_tensor_slices([1, 2, 3])
my_iter = iter(dataset)
c = C(my_iter)
length = len(dataset)

for _ in range(length):
    element = next(c.some_iter)
    add(element, element)
