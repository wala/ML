import tensorflow as tf


def f(a):
    pass


x = tf.convert_to_tensor([1, 2, 3, 4, 5])
y = tf.convert_to_tensor(1)
z = tf.add(x, y)
f(z)
