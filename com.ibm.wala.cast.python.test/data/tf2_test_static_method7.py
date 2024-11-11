import tensorflow as tf


class MyClass:

    @tf.function
    def the_static_method(self, x):
        assert isinstance(x, tf.Tensor)


MyClass().the_static_method(tf.constant(1))
