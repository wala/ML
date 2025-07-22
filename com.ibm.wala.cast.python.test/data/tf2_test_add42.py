import tensorflow as tf
from tensorflow.python.framework.ops import Tensor


def func2(t):
    assert isinstance(t, tf.Tensor)


@tf.function
def func():
    a = tf.constant([[1.0, 2.0], [3.0, 4.0]])
    b = tf.constant([[1.0, 1.0], [0.0, 1.0]])
    c = tf.matmul(a, b)
    tensor = Tensor(c.op, 0, tf.float32)
    func2(tensor)


func()
