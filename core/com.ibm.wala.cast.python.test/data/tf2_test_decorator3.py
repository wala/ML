import tensorflow as tf


@tf.function(input_signature=(tf.TensorSpec(shape=[None], dtype=tf.float32),))
@tf.function(reduce_retracing=True)
def returned(a):
    return a


a = tf.constant([1.0, 1.0])
b = returned(a)
