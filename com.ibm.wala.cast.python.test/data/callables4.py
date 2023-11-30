class C(object):

    def __call__(self, x):
        return x * x


c = C()
a = c(5)
assert a == 25
