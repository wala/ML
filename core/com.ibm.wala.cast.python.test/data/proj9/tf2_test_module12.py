# Test https://github.com/wala/ML/issues/163.

from tensorflow import ones
from src.tf2_test_module11b import D

D().g(ones([1, 2]))
