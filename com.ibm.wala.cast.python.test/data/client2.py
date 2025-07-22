# Test https://github.com/wala/ML/issues/211.

from tensorflow import ones
from module import f

f(ones([1, 2]))
