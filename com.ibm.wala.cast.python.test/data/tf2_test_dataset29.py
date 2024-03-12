# From https://www.tensorflow.org/versions/r2.9/api_docs/python/tf/data/Dataset#choose_from_datasets.

import tensorflow as tf


def f(a):
    assert isinstance(a, tf.Tensor)


datasets = [
    tf.data.Dataset.from_tensors("foo").repeat(),
    tf.data.Dataset.from_tensors("bar").repeat(),
    tf.data.Dataset.from_tensors("baz").repeat(),
]

# Define a dataset containing `[0, 1, 2, 0, 1, 2, 0, 1, 2]`.
choice_dataset = tf.data.Dataset.range(3).repeat(3)

result = tf.data.Dataset.choose_from_datasets(datasets, choice_dataset)

for element in result:
    f(element)
