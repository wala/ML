from keras.engine.input_layer import Input


def add(a, b):
    return a + b


c = add(Input(shape=(32,)), Input(shape=(32,)))
