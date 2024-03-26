# Test https://github.com/wala/ML/issues/163.

from tensorflow import ones
from tf2_test_module2 import f

f(ones([1, 2]))
