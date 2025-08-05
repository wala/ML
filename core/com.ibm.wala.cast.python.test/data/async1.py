import asyncio

async def fibonacci(x):
  assert x >= 0
  if (x == 0):
      return 0
  elif (x <= 2):
    return 1
  else:
    ol = await fibonacci(x - 1)
    tl = await fibonacci(x - 2)
    return ol+tl

asyncio.run(fibonacci(10))

