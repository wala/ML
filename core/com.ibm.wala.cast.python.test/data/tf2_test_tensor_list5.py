import tensorflow as tf


def add(a, b):
    return a + b


my_list = list([tf.ones([1, 2]), tf.ones([2, 2])])

for element in my_list:
    c = add(element, element)
