# From https://www.tensorflow.org/versions/r2.9/api_docs/python/tf/distribute/Strategy#example_usage_2.

import tensorflow as tf

tensor_input = tf.constant(3.0)


@tf.function
def replica_fn(input):
  return input * 2.0


# Direct call.
result = replica_fn((tensor_input,)[0])
print(result)
