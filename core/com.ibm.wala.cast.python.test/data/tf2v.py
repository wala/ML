import tensorflow as tf


def add(a, b):
    return a + b


c = add(tf.keras.Input(shape=(32,)), tf.keras.Input(shape=(32,)))
