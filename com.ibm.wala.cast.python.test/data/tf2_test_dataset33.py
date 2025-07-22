# From https://www.tensorflow.org/versions/r2.9/api_docs/python/tf/data/Dataset#sample_from_datasets.

import tensorflow as tf


def f(a):
    assert isinstance(a, tf.Tensor)


dataset1 = tf.data.Dataset.range(0, 3)
dataset2 = tf.data.Dataset.range(100, 103)

sample_dataset = tf.data.Dataset.sample_from_datasets(
    [dataset1, dataset2], weights=[0.5, 0.5]
)

for element in sample_dataset:
    f(element)
