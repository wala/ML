# Test https://github.com/wala/ML/issues/163.

from tensorflow import ones
import pytest
from src.tf2_test_module9b import D


@pytest.mark.parametrize("test_input,expected", [("3+5", 8), ("2+4", 6), ("6*9", 42)])
def test_dummy(x, test_input, expected):
    D().f(ones([1, 2]))
