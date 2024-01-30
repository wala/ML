import tensorflow
from tensorflow import Tensor


def func2(t):
    pass


@tensorflow.function
def func():
    a = tensorflow.constant([[1.0, 2.0], [3.0, 4.0]])
    b = tensorflow.constant([[1.0, 1.0], [0.0, 1.0]])
    c = tensorflow.matmul(a, b)
    tensor = Tensor(c.op, 0, tensorflow.float32)
    func2(tensor)


func()
