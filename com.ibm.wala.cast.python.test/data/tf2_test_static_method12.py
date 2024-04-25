import tensorflow as tf


def f(x):
  assert isinstance(x, tf.Tensor)


class MyClass:

  @staticmethod
  def the_static_method(x):
    assert isinstance(x, tf.Tensor)
    f(x)


MyClass().the_static_method(tf.constant(1))
