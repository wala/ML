# Test https://github.com/wala/ML/issues/163.

from tensorflow import ones
from C.B import f

f(ones([1, 2]))
