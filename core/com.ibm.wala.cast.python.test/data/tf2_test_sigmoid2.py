import tensorflow as tf


def f(a):
    pass


x = tf.constant([0.0, 1.0, 50.0, 100.0])
y = tf.nn.sigmoid(x)
f(y)
