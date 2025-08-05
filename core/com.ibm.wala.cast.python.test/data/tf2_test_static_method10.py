import tensorflow as tf


class MyClass:

    @staticmethod
    def the_static_method(x, y):
        assert isinstance(x, tf.Tensor)
        assert isinstance(y, tf.Tensor)


MyClass().the_static_method(tf.constant(1), tf.constant(2))
