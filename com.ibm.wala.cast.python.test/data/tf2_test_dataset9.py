import tensorflow as tf


def add(a, b):
    return a + a


def func(ds):
    for images, labels in ds:
        c = add(images, labels)


(x_train, y_train), (x_test, y_test) = tf.keras.datasets.mnist.load_data()
dataset = (
    tf.data.Dataset.from_tensor_slices((x_train, y_train)).shuffle(10000).batch(32)
)
func(dataset)
