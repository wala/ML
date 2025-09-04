import tensorflow as tf


@tf.function(experimental_follow_type_hints=True)
def returned(a):
    return a


a = tf.range(5)
b = returned(a)
