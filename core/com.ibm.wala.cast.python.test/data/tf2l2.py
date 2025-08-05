import tensorflow
from tensorflow.python.framework.ops import convert_to_tensor


def add(a, b):
    return a + b


c = add(tensorflow.convert_to_tensor(1), convert_to_tensor(2))
