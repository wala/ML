# Complete code
import tensorflow as tf

class GAAEConv(tf.keras.layers.Layer):
    def __init__(self, num_heads, num_messages):
        super(GAAEConv, self).__init__()
        self.num_heads = num_heads
        self.num_messages = num_messages

    def build(self, input_shape):
        self.message_function = tf.keras.layers.Dense(self.num_heads, activation='relu')

    def call(self, inputs):
        updates = tf.ones_like(inputs, dtype=tf.float32)
        indices = tf.range(tf.shape(inputs)[0])[:, None]
        output = tf.scatter_nd(indices, updates, tf.shape(inputs), dtype=tf.float32)
        return tf.stack([self.message_function(output[:, i]) for i in range(self.num_heads)], axis=-1)

class GATConv(tf.keras.layers.Layer):
    def __init__(self, num_heads, num_messages):
        super(GATConv, self).__init__()
        self.num_heads = num_heads
        self.num_messages = num_messages

    def build(self, input_shape):
        self.message_function = tf.keras.layers.Dense(self.num_heads, activation='relu')

    def call(self, inputs):
        updates = tf.ones_like(inputs, dtype=tf.float32)
        indices = tf.range(tf.shape(inputs)[0])[:, None]
        output = tf.scatter_nd(indices, updates, tf.shape(inputs), dtype=tf.float32)
        return tf.stack([self.message_function(output[:, i]) for i in range(self.num_heads)], axis=-1)

class GSConv(tf.keras.layers.Layer):
    def __init__(self, num_heads, num_messages):
        super(GSConv, self).__init__()
        self.num_heads = num_heads
        self.num_messages = num_messages

    def build(self, input_shape):
        self.message_function = tf.keras.layers.Dense(self.num_heads, activation='relu')

    def call(self, inputs):
        updates = tf.ones_like(inputs, dtype=tf.float32)
        indices = tf.range(tf.shape(inputs)[0])[:, None]
        output = tf.scatter_nd(indices, updates, tf.shape(inputs), dtype=tf.float32)
        return tf.stack([self.message_function(output[:, i]) for i in range(self.num_heads)], axis=-1)

class TGCNConv(tf.keras.layers.Layer):
    def __init__(self, num_heads, num_messages):
        super(TGCNConv, self).__init__()
        self.num_heads = num_heads
        self.num_messages = num_messages

    def build(self, input_shape):
        self.message_function = tf.keras.layers.Dense(self.num_heads, activation='relu')

    def call(self, inputs):
        updates = tf.ones_like(inputs, dtype=tf.float32)
        indices = tf.range(tf.shape(inputs)[0])[:, None]
        output = tf.scatter_nd(indices, updates, tf.shape(inputs), dtype=tf.float32)
        return tf.stack([self.message_function(output[:, i]) for i in range(self.num_heads)], axis=-1)

def message_function_gaaeconv(inputs, num_heads, num_messages):
    updates = tf.ones_like(inputs, dtype=tf.float32)
    indices = tf.range(tf.shape(inputs)[0])[:, None]
    output = tf.scatter_nd(indices, updates, tf.shape(inputs), dtype=tf.float32)
    return tf.stack([tf.keras.layers.Dense(num_heads, activation='relu')(output[:, i]) for i in range(num_heads)], axis=-1)

def message_function_gatconv(inputs, num_heads, num_messages):
    updates = tf.ones_like(inputs, dtype=tf.float32)
    indices = tf.range(tf.shape(inputs)[0])[:, None]
    output = tf.scatter_nd(indices, updates, tf.shape(inputs), dtype=tf.float32)
    return tf.stack([tf.keras.layers.Dense(num_heads, activation='relu')(output[:, i]) for i in range(num_heads)], axis=-1)

def message_function_gsconv(inputs, num_heads, num_messages):
    updates = tf.ones_like(inputs, dtype=tf.float32)
    indices = tf.range(tf.shape(inputs)[0])[:, None]
    output = tf.scatter_nd(indices, updates, tf.shape(inputs), dtype=tf.float32)
    return tf.stack([tf.keras.layers.Dense(num_heads, activation='relu')(output[:, i]) for i in range(num_heads)], axis=-1)

def message_function_tgcnconv(inputs, num_heads, num_messages):
    updates = tf.ones_like(inputs, dtype=tf.float32)
    indices = tf.range(tf.shape(inputs)[0])[:, None]
    output = tf.scatter_nd(indices, updates, tf.shape(inputs), dtype=tf.float32)
    return tf.stack([tf.keras.layers.Dense(num_heads, activation='relu')(output[:, i]) for i in range(num_heads)], axis=-1)