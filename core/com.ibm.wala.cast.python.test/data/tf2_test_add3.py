import tensorflow as tf


def f(a):
    pass


x = [1, 2, 3, 4, 5]
y = tf.constant([1, 2, 3, 4, 5])
z = tf.add(x, y)
f(z)
