import tensorflow as tf


def add(a, b):
  return a + b


dataset = tf.data.Dataset(None).shuffle(3)  # This is actually illegal since this ctor is not publicly visible.

for element in dataset:
    c = add(element, element)
