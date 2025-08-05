from tensorflow import keras


def add(a, b):
    return a + b


c = add(keras.Input(shape=(32,)), keras.Input(shape=(32,)))
