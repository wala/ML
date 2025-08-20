import tensorflow as tf


class C:

    @tf.function()
    def returned(self, a):
        assert isinstance(a, tf.Tensor)
        return a


a = tf.range(5)
assert isinstance(a, tf.Tensor)
c = C()
b = c.returned(a)
assert isinstance(b, tf.Tensor)
