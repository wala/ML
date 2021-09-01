import pytest

@pytest.mark.parametrize(
    "index", [[1, 2], [1.0, 2.0], ["a", "b"], ["1", "2"], ["1.", "2."]]
)
def test_param_one(index):
    def foo(x):
        print(type(x[0]))
        
    foo(index)

def test_no_params():
    def none():
        print("none")

    none()
    
def some_callee():
    return "some callee"

@pytest.fixture
def testparam():
    return some_callee

def test_param_local(testparam):
    print("testparam is " + str(testparam()))
    
class TestTestClass:
    def show(self, x):
        print(str(x))

    @pytest.mark.parametrize("value", [1, "a", True])
    def test_param_two(self, value):
        self.show(value)


class notATestClass:

    @pytest.mark.parametrize("value", [1, "a", True])
    def test_param_three(self, value):
        print(value)

@pytest.mark.parametrize(
    "index", [[1, 2], [1.0, 2.0], ["a", "b"], ["1", "2"], ["1.", "2."]]
)
@pytest.mark.parametrize(
    "columns", [["a", "b"], ["1", "2"], ["1.", "2."]]
)
def test_param_three(index, columns):
    def f(x, y):
        print("(" + str(x) + "," + str(y) + ")")
        
    for x in index:
        for y in columns:
            f(x, y)

import pandas_shim as pd

@pytest.mark.parametrize(
    "name, expected_obj",[("pandas.isnull", pd.isnull), ("pandas.DataFrame", pd.DataFrame)]
)
def test_param_four(name, expected_obj):
    print("(" + str(name) + "," + str(expected_obj(name)) + ")")

@pytest.mark.parametrize(
    "values", [pd.Categorical([]), pd.Categorical([]).dtype, pd.Series(pd.Categorical([]))]
)
def test_param_five(values):
    def f(x):
        print(str(x))
        
    f(values)
    
@pytest.mark.parametrize("ascending", [True, False])
def test_param_six(ascending):
    def f(x):
        print(str(x))

    f(ascending)
    
@pytest.mark.parametrize("box", [pd.Series, lambda x: x])
def test_box(box):
    def f(x):
        print(str(x(0)))

    f(box)
    
@pytest.mark.parametrize("repeats", [0, 1, 2, [1, 2, 3]])
def test_repeat(repeats):
    def f(x):
        print(str(x))

    f(repeats)

from pandas_shim import DecimalArrayWithoutFromSequence, DecimalArrayWithoutCoercion

@pytest.mark.parametrize("class_", [DecimalArrayWithoutFromSequence, DecimalArrayWithoutCoercion])
def test_class(class_):
     print(class_().foo())

@pytest.mark.parametrize("columns",[["A", "B"], pd.MultiIndex.from_tuples([("A", "a"), ("A", "b")], names=["outer", "inner"])])
def test_cols(columns):
     print("(" + str(columns) + ")")

#@pytest.mark.parametrize("repeats, kwargs, error, msg",[
#(2, dict(axis=1), ValueError, "'axis"),
#(-1, dict(), ValueError, "negative"),
#([1, 2], dict(), ValueError, "shape")])
#def test_dict(repeats, kwargs, error, msg):
#     print("(" + str(repeats) + " " + str(kwargs) + " " + str(error) + " " + msg + ")")

from pandas_shim import DataFrame
import np_shim as np

@pytest.mark.parametrize("header,expected",[(None, DataFrame([0] + [np.nan] * 4)), (0, DataFrame([np.nan] * 4))])
def test_head_expected(header, expected):
     print("(" + str(header) + " " + str(expected))


@pytest.mark.parametrize("option,result,expected",[ (None, lambda df: df.to_html(), "1"), (None, lambda df: df.to_html(border=0), "0"), (0, lambda df: df.to_html(), "0")])
def test_lamda_call(option, result, expected):
    df = DataFrame({"A": [1, 2]})
    print("(" + str(option) + " " + str(result(df)) + " " + str(expected))

                           
