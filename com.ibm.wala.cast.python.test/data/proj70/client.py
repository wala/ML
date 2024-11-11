# Test https://github.com/wala/ML/issues/210.

from tensorflow import ones
import src.module

src.module.f(ones([1, 2]))
