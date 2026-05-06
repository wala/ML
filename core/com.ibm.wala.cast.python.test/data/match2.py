def monday():
    print("Monday")


def tuesday():
    print("Tuesday")


def wednesday():
    print("Wednesday")


def thursday():
    print("Thursday")


def friday():
    print("Friday")


def saturday():
    print("Saturday")


def sunday():
    print("Sunday")


def otherDay():
    print("other day??")


def doit(month):
    for day in [1, 2, 3, 4, 5, 6, 7, otherDay]:
        match day:
            case 1 if 1 <= month <= 12:
                monday()
            case 2 if 1 <= month <= 12:
                tuesday()
            case 3 if 1 <= month <= 12:
                wednesday()
            case 4 if 1 <= month <= 12:
                thursday()
            case 5 if 1 <= month <= 12:
                friday()
            case 6 if 1 <= month <= 12:
                saturday()
            case 7 if 1 <= month <= 12:
                sunday()
            case x if callable(x):
                x()


doit(4)
doit(0)
