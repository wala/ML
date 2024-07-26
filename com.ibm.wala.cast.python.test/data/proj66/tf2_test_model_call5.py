# Test https://github.com/wala/WALA/discussions/1417#discussioncomment-10085680.

import tensorflow as tf
from src.tf2_test_model_call5b import g

# Create an override model to classify pictures


class SequentialModel(tf.keras.Model):

    def __init__(self, **kwargs):
        super(SequentialModel, self).__init__(**kwargs)

        self.flatten = tf.keras.layers.Flatten(input_shape=(28, 28))

        # Add a lot of small layers
        num_layers = 100
        self.my_layers = [
            tf.keras.layers.Dense(64, activation="relu") for n in range(num_layers)
        ]

        self.dropout = tf.keras.layers.Dropout(0.2)
        self.dense_2 = tf.keras.layers.Dense(10)

    def __call__(self, x):
        print("Raffi 1")
        x = self.flatten(x)

        for layer in self.my_layers:
            x = layer(x)

        x = self.dropout(x)
        x = self.dense_2(x)

        return x

    def predict(self, x):
        return self(x)


input_data = tf.random.uniform([20, 28, 28])

model = SequentialModel()
result = g(model, input_data)
