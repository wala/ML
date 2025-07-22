# From https://blog.teclado.com/python-abc-abstract-base-classes/#introducing-abstract-classes.
import tensorflow as tf
from abc import ABC, abstractmethod


class C(ABC):

    @abstractmethod
    def f(self, x):
        assert isinstance(x, tf.Tensor)


class D(C):

    def f(self, x):
        super(D, self).f(x)


c = D()
c.f(tf.constant(1))
