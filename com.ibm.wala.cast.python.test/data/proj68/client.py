# Test https://github.com/wala/ML/issues/210.

from tensorflow import ones
from src import f

f(ones([1, 2]))
