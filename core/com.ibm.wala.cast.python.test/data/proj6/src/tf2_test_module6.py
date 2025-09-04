# Test https://github.com/wala/ML/issues/163.

from tensorflow import ones
from tf2_test_module8a import D

D().g(ones([1, 2]))
