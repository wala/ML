import asyncio

def f1(a):
    return lambda x: a + x


def f2(a):
    return lambda x: a * x


def f3(a):
    return lambda x: a - x


async def gen():
    yield f1
    yield f2
    yield f3


def p1(f, i):
    return f(i)
    
def p2(f, i):
    print(f(i))
    
async def main():
    i = 0
    async for f in gen():
        g = p1(f, i)
        p2(g, i)
        i = i + 1

asyncio.run(main())
