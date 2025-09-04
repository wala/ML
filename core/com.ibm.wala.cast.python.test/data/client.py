# Test https://github.com/wala/ML/issues/211.

from tensorflow import ones
import module

module.f(ones([1, 2]))
