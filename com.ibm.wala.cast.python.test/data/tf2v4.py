from keras.engine.input_layer import Input
import tensorflow as tf

def add(a, b):
  return a + b


c = add(tf.keras.Input(shape=(32,)), Input(shape=(32,)))
