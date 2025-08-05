class C(object):
    def __call__(self, x):
        return x * x


c = C()
a = c.__call__(5)
assert a == 25
