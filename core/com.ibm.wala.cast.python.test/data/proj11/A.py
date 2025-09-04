# Test https://github.com/wala/ML/issues/163.

from tensorflow import ones
from C import B

B.f(ones([1, 2]))
