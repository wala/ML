# Test https://github.com/wala/ML/issues/210.

from tensorflow import ones
import src

src.f(ones([1, 2]))
