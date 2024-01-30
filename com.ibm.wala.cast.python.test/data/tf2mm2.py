import tensorflow


def add(a, b):
    return a + b


c = add(
    tensorflow.keras.layers.Input(shape=(32,)),
    tensorflow.keras.layers.Input(shape=(32,)),
)
