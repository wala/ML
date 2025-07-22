import tensorflow as tf


class MyClass:

    def f(x):
        assert isinstance(x, tf.Tensor)

    @classmethod
    def the_class_method(cls, x):
        assert isinstance(x, tf.Tensor)
        cls.f(x)


MyClass().the_class_method(tf.constant(1))
