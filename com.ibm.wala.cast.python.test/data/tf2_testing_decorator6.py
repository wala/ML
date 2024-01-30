import tensorflow as tf


@tf.function(experimental_implements="google.matmul_low_rank_matrix")
def returned(a):
    return a


a = tf.range(5)
b = returned(a)
