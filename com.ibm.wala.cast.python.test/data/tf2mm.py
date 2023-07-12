import tensorflow as tf

def add(a, b):
  return a + b


c = add(tf.keras.layers.Input(shape=(32,)), tf.keras.layers.Input(shape=(32,)))
