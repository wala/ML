# Test https://github.com/wala/ML/issues/163.

from tensorflow import ones
from tf2_test_module6 import D

D().f(ones([1, 2]))
