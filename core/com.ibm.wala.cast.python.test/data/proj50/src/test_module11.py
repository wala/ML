# Test https://github.com/wala/ML/issues/163.

from tensorflow import ones
import pytest
from src.tf2_test_module10b import D


@pytest.mark.parametrize
def test_dummy(x, test_input, expected):
    D().f(ones([1, 2]))
