def isnull(x):
    return False


class DataFrame:
    def __init__(self, x):
        self.x = x

    def to_html(self, border=None):
        return self.x


class Series:
    def __init__(self, x):
        self.x = x

    def sum():
        return 0


class Categorical:
    def __init__(self, x):
        self.dtype = "type"


class DecimalArrayWithoutFromSequence:
    def foo(self):
        print("a")


class DecimalArrayWithoutCoercion:
    def foo(self):
        print("b")


class MultiIndex:
    @staticmethod
    def from_tuples(a, names):
        return a
