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


async def main():
    i = 0
    async for f in gen():
        g = f(i)
        print(g(i))
        i = i + 1

asyncio.run(main())
