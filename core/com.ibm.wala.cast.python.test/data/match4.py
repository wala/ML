def weekday(day):
    print("week day " + str(day))


def weekend(day):
    print("week end " + str(day))


def otherDay():
    print("other day??")


def somethingElse():
    print("something else")


def mten():
    return 10


def mseven():
    return 10


def doit(dmf):
    match dmf:
        case [1 | 2 | 3 | 4 | 5 as day, month] if 1 <= month() <= 12:
            weekday(day)
        case [6 | 7 as day, month] if 1 <= month() <= 12:
            weekend(day)
        case x if callable(x):
            x()
        case _:
            somethingElse()


doit((3, mseven))
doit([7, mten])
doit([8, mten])
doit(otherDay)
doit(0)
