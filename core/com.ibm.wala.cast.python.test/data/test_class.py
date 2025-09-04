# From: https://docs.pytest.org/en/8.0.x/getting-started.html#group-multiple-tests-in-a-class.


# content of test_class.py
class TestClass:

    def test_one(self):
        x = "this"
        assert "h" in x

    def test_two(self):
        x = "hello"
        assert hasattr(x, "check")
