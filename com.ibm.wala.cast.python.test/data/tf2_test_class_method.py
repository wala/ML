import tensorflow as tf


class MyClass:

  @classmethod
  def the_class_method(cls, x):
    assert isinstance(x, tf.Tensor)


MyClass.the_class_method(tf.constant(1))
