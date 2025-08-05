# from https://www.tensorflow.org/versions/r2.9/api_docs/python/tf/nn/sparse_softmax_cross_entropy_with_logits

import tensorflow as tf


def f(a):
    pass


logits = tf.constant(
    [[2.0, -5.0, 0.5, -0.1], [0.0, 0.0, 1.9, 1.4], [-100.0, 100.0, -100.0, -100.0]]
)
labels = tf.constant([0, 3, 1])
f(tf.nn.sparse_softmax_cross_entropy_with_logits(labels=labels, logits=logits.numpy()))
