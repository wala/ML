import tensorflow as tf


def add(a, b):
    return a + b


list = list()

list.append(tf.ones([1, 2]))
list.append(tf.ones([2, 2]))

for element in list:
    c = add(element, element)
