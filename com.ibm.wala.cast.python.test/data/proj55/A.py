# Test https://github.com/wala/ML/issues/163.

from tensorflow import ones
from src import C

C().f(ones([1, 2]))
