from keras.engine import input_layer
import tensorflow as tf


def add(a, b):
    return a + b


c = add(tf.keras.Input(shape=(32,)), input_layer.Input(shape=(32,)))
