# Test https://github.com/wala/ML/issues/209.
import tensorflow as tf
from models import f
from models import g

f(tf.constant(1))
g(tf.constant(1))
