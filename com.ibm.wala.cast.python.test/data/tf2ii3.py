from tensorflow import random
from tensorflow.python.ops.random_ops import random_normal
import tensorflow as tf

def add(a, b):
  return a + b


c = add(random.normal([4], 0, 1, tf.float32), random_normal([4], 2, 1, tf.float32))
