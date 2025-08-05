# From https://blog.teclado.com/python-abc-abstract-base-classes/#introducing-abstract-classes.
import tensorflow as tf
from abc import ABC, abstractmethod


class C(ABC):

    @abstractmethod
    def f(self, x):
        assert isinstance(x, tf.Tensor)


c = C()
c.f(tf.constant(1))
