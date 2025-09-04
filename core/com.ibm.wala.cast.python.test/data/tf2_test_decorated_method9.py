import tensorflow as tf


@mama
def f(x):
    assert isinstance(x, tf.Tensor)


f(tf.constant(1))
