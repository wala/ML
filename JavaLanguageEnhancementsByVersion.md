# Java Language Versions
## Java 8
### lambda
- Sorting an array in java with the use of lambda
  ```
  int[] numbers = {5, 2, 8, 1, 9};
  // Sort in ascending order
  Arrays.sort(numbers, (a, b) -> a - b);
  ```
### default methods
### Improved type inference
- List takes in a collection but the type passed in is a list and it can infer and pass it in correctly
  ```
  List<String> stringList = new ArrayList<>();
  stringList.add("A");
  stringList.addAll(Arrays.asList());
  ```