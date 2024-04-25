import tensorflow as tf


class MyClass:

  @staticmethod
  def the_static_method(x):
    assert isinstance(x, tf.Tensor)


MyClass.the_static_method(tf.constant(1))
