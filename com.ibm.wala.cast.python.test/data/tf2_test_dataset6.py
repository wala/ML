import tensorflow as tf


def add(a, b):
    return a + b


def func(ds):
    for element in ds:
        c = add(element, element)


dataset = tf.data.Dataset.from_tensor_slices([1, 2, 3]).shuffle(3).batch(2)
func(dataset)
