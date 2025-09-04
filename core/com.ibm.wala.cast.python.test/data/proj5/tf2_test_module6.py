# Test https://github.com/wala/ML/issues/163.

from tensorflow import ones
from src.tf2_test_module5a import C

C().f(ones([1, 2]))
