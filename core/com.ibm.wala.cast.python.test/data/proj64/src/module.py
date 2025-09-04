# Test https://github.com/wala/ML/issues/163.

from tensorflow import Tensor
from typing import NamedTuple, List
import tensorflow as tf


class GNNInput(NamedTuple):
    node_embeddings: tf.Tensor
    adjacency_lists: List
    edge_weights: List
    etan: tf.Tensor


def f(a):
    assert isinstance(a, Tensor)
