import tensorflow
import pytest
import sys


@tensorflow.autograph.experimental.do_not_convert
def dummy_fun(a):
    pass


@pytest.mark.parametrize("test_input,expected", [("3+5", 8), ("2+4", 6), ("6*9", 42)])
def dummy_test(x, test_input, expected):
    pass


@pytest.mark.skipif(sys.version_info < (3, 10), reason="requires python3.10 or higher")
def test_function(x):
    pass


@pytest.mark.skip(reason="requires python3.10 or higher")
def test_function2(x):
    pass


@pytest.mark.skip("requires python3.10 or higher")
def test_function3(x):
    pass


@pytest.mark.skip
def test_function4(x):
    pass


dummy_fun(tensorflow.constant(1))
dummy_test(tensorflow.constant(1), "1", "1")
test_function(tensorflow.constant(1))
test_function2(tensorflow.constant(1))
test_function3(tensorflow.constant(1))
test_function4(tensorflow.constant(1))
