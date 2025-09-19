def weekday():
    print("week day")


def weekend():
    print("week end")


def otherDay():
    print("other day??")


def doit(month):
    for day in [1, 2, 3, 4, 5, 6, 7, otherDay]:
        match day:
            case 1 | 2 | 3 | 4 | 5 if 1 <= month <= 12:
                weekday()
            case 6 | 7 if 1 <= month <= 12:
                weekend()
            case x if callable(x):
                x()


doit(4)
doit(0)
