# Test https://github.com/wala/WALA/discussions/1417#discussioncomment-10085680.


def f(m, d):
    return m.predict(d)


def g(m, d):
    return f(m, d)
