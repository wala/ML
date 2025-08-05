# Test https://github.com/wala/ML/issues/163.

from tensorflow import ones
from src.tf2_test_module10b import D

D().f(ones([1, 2]))
