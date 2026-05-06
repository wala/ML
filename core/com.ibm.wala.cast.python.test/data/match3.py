def weekday(day):
    print("week day " + str(day))


def weekend(day):
    print("week end " + str(day))


def otherDay():
    print("other day??")


def somethingElse():
    print("something else")


def doit(month):
    for day in [1, 2, 3, 4, 5, 6, 7, otherDay]:
        match day:
            case 1 | 2 | 3 | 4 | 5 as day if 1 <= month <= 12:
                weekday(day)
            case 6 | 7 as day if 1 <= month <= 12:
                weekend(day)
            case x if callable(x):
                x()
            case _:
                somethingElse()


doit(4)
doit(0)
