# From https://www.tensorflow.org/versions/r2.9/api_docs/python/tf/distribute/Strategy#example_usage_2.

import tensorflow as tf

strategy = tf.distribute.MirroredStrategy(["GPU:0", "GPU:1"])
tensor_input = tf.constant(3.0)


@tf.function
def replica_fn(input):
  return input * 2.0


# Indirect call to replica_fun().
result = strategy.run(replica_fn, (tensor_input,))
print(result)
