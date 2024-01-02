import tensorflow as tf


def add(a, b):
    return a + b


dataset = tf.data.Dataset.from_tensor_slices([1, 2, 3]).shuffle(3).batch(2)

for element in dataset:
    c = add(element, element)
