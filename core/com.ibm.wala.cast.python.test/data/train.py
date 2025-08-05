# Following along here:
#  https://github.com/tensorflow/tensorflow/blob/master/tensorflow/examples/how_tos/reading_data/fully_connected_reader.py

# Also useful:
#  https://github.com/tensorflow/tensorflow/blob/master/tensorflow/examples/how_tos/reading_data/convert_to_records.py

import tensorflow as tf
import os, time

# IMAGE_PIXELS=256*256*256
# IMAGE_PIXELS_3D_SINGLE_CHAN=[256,256,256,1]

INPUT_DIR = "/notebooks/data"
CHECKPOINT_DIR = "/notebooks/data"
TRAIN_FILE = "b40-train.tfrecords"
VALIDATION_FILE = ""
IMG_DIM = 40
NUM_CLASSES = 2
BATCH_SIZE = 2
NUM_EPOCHS = 2
DECAY_STEPS = 1.0
DECAY_RATE = 1.0
LEARNING_RATE = 1.0


def _bytes_feature(value):
    return tf.train.Feature(bytes_list=tf.train.BytesList(value=[value]))


def _int64_feature(value):
    return tf.train.Feature(int64_list=tf.train.Int64List(value=[value]))


def print_tensor_shape(tensor, string):
    if __debug__:
        print("DEBUG " + string, tensor.get_shape())


def read_and_decode(filename_queue):
    reader = tf.TFRecordReader()
    _, serialized_example = reader.read(filename_queue)
    features = tf.parse_single_example(
        serialized_example,
        features={
            "image_raw": tf.FixedLenFeature([], tf.string),
            "label_raw": tf.FixedLenFeature([], tf.string),
        },
    )
    imagei = tf.cast(tf.decode_raw(features["image_raw"], tf.int16), tf.float32)
    labels = tf.decode_raw(features["label_raw"], tf.int16)

    # PW 2017/03/03: Zero-center data here?
    imagei.set_shape([IMG_DIM * IMG_DIM * IMG_DIM])
    image = tf.reshape(imagei, [IMG_DIM, IMG_DIM, IMG_DIM, 1])

    labels.set_shape([IMG_DIM * IMG_DIM * IMG_DIM])
    labels = tf.reshape(image, [IMG_DIM, IMG_DIM, IMG_DIM])

    # Dimensions (X, Y, Z, channles)
    return image, labels


def inputs(train, batch_size, num_epochs, filename):
    """
    Reads input data num_epochs times.
    Args:
            train: Selects between the training (True) and validation (False) data.
            batch_size: Number of examples per returned batch.
            num_epochs: Number of times to read the input data, or 0/None to train forever.
    Returns:
            A tuple (images, labels), where:
                    * images is a float tensor with shape [batch_size, mnist.IMAGE_PIXELS]
                    in the range [-0.5, 0.5].
                    * labels is an int32 tensor with shape [batch_size] with the true label,
                    a number in the range [0, mnist.NUM_CLASSES).
            Note that an tf.train.QueueRunner is added to the graph, which
            must be run using e.g. tf.train.start_queue_runners().
    """
    if not num_epochs:
        num_epochs = None

    with tf.name_scope("input"):
        filename_queue = tf.train.string_input_producer(
            [filename], num_epochs=num_epochs
        )

    # Even when reading in multiple threads, share the filename queue.
    image, label = read_and_decode(filename_queue)

    # Shuffle the examples and collect them into batch_size batches.
    # (Internally uses a RandomShuffleQueue.)
    # We run this in two threads to avoid being a bottleneck.
    images, sparse_labels = tf.train.shuffle_batch(
        [image, label],
        batch_size=batch_size,
        num_threads=2,
        capacity=1000 + 3 * batch_size,
        min_after_dequeue=1000,
    )

    # Dimensions (batchsize, X, Y, Z, channles)
    return images, sparse_labels


def inference(images):
    # Convolution layer (https://www.tensorflow.org/api_docs/python/tf/nn/conv3d)
    # tf.nn.conv3d(input, filter, strides, padding, name=None)
    #    input shape: [batch, depth, height, width, in_channels]
    #  	 filter shape: [filter_depth, filter_height, filter_width, in_channels, out_channels]
    #    strides shape [1, ?, ?, ?, 1]

    # Pool layer (https://www.tensorflow.org/api_docs/python/tf/nn/max_pool3d)
    # tf.nn.max_pool3d(input, ksize, strides, padding, name=None)
    #    input shape: [batch, depth, height, width, channels]
    #    ksize: The size of the window for each dimension of the input tensor.
    #           Must have ksize[0] = ksize[4] = 1
    #    strides shape [1, ?, ?, ?, 1]

    print_tensor_shape(images, "images shape inference")
    with tf.name_scope("Conv1"):
        W_conv1 = tf.Variable(
            tf.truncated_normal([3, 3, 3, 1, 10], stddev=0.1, dtype=tf.float32),
            name="W_conv1",
        )
        print_tensor_shape(W_conv1, "W_conv1 shape")
        conv1_op = tf.nn.conv3d(
            images, W_conv1, strides=[1, 2, 2, 2, 1], padding="SAME", name="conv1_op"
        )
        print_tensor_shape(conv1_op, "conv1_op shape")
        relu1_op = tf.nn.relu(conv1_op, name="relu1_op")
        print_tensor_shape(relu1_op, "relu1_op shape")
    with tf.name_scope("Pool1"):
        pool1_op = tf.nn.max_pool3d(
            relu1_op, ksize=[1, 3, 3, 3, 1], strides=[1, 2, 2, 2, 1], padding="SAME"
        )
        print_tensor_shape(pool1_op, "pool1_op shape")
    with tf.name_scope("Conv2"):
        W_conv2 = tf.Variable(
            tf.truncated_normal([3, 3, 3, 10, 100], stddev=0.1, dtype=tf.float32),
            name="W_conv2",
        )
        print_tensor_shape(W_conv2, "W_conv2 shape")
        conv2_op = tf.nn.conv3d(
            pool1_op, W_conv2, strides=[1, 2, 2, 2, 1], padding="SAME", name="conv2_op"
        )
        print_tensor_shape(conv2_op, "conv2_op shape")
        relu2_op = tf.nn.relu(conv2_op, name="relu2_op")
        print_tensor_shape(relu2_op, "relu2_op shape")
    with tf.name_scope("Pool2"):
        pool2_op = tf.nn.max_pool3d(
            relu2_op, ksize=[1, 3, 3, 3, 1], strides=[1, 2, 2, 2, 1], padding="SAME"
        )
        print_tensor_shape(pool2_op, "pool2_op shape")
    with tf.name_scope("Conv3"):
        W_conv3 = tf.Variable(
            tf.truncated_normal([3, 3, 3, 100, 200], stddev=0.1, dtype=tf.float32),
            name="W_conv3",
        )
        print_tensor_shape(W_conv3, "W_conv3 shape")
        conv3_op = tf.nn.conv3d(
            pool2_op, W_conv3, strides=[1, 2, 2, 2, 1], padding="SAME", name="conv3_op"
        )
        print_tensor_shape(conv3_op, "conv3_op shape")
        relu3_op = tf.nn.relu(conv3_op, name="relu3_op")
        print_tensor_shape(relu3_op, "relu3_op shape")
    with tf.name_scope("Conv4"):
        W_conv4 = tf.Variable(
            tf.truncated_normal([3, 3, 3, 200, 200], stddev=0.1, dtype=tf.float32),
            name="W_conv4",
        )
        print_tensor_shape(W_conv4, "W_conv4 shape")
        conv4_op = tf.nn.conv3d(
            relu3_op, W_conv4, strides=[1, 2, 2, 2, 1], padding="SAME", name="conv4_op"
        )
        print_tensor_shape(conv4_op, "conv4_op shape")
        relu4_op = tf.nn.relu(conv4_op, name="relu4_op")
        print_tensor_shape(relu4_op, "relu4_op shape")
        # optional dropout node.  when set to 1.0 nothing is dropped out
        drop_op = tf.nn.dropout(relu4_op, 1.0)
        print_tensor_shape(drop_op, "drop_op shape")
    # Conv layer to generate the 2 score classes
    with tf.name_scope("Score_classes"):
        W_score_classes = tf.Variable(
            tf.truncated_normal([1, 1, 1, 200, 2], stddev=0.1, dtype=tf.float32),
            name="W_score_classes",
        )
        print_tensor_shape(W_score_classes, "W_score_classes_shape")
        score_classes_conv_op = tf.nn.conv3d(
            drop_op,
            W_score_classes,
            strides=[1, 1, 1, 1, 1],
            padding="SAME",
            name="score_classes_conv_op",
        )
        print_tensor_shape(score_classes_conv_op, "score_conv_op shape")
    # Upscore the results to 1x256x256x256x2 image
    #  Deconv3d https://www.tensorflow.org/api_docs/python/tf/nn/conv3d_transpose
    #   tf.nn.conv3d_transpose(value, filter, output_shape, strides, padding='SAME', name=None)
    #     value: A 5-D Tensor of type float and shape [batch, depth, height, width, in_channels]
    #     filter: A 5-D Tensor with the same type as value and shape [depth, height, width, output_channels, in_channels]. filter's in_channels dimension must match that of value.
    #     output_shape: A 1-D Tensor representing the output shape of the deconvolution op.
    #     strides: A list of ints. The stride of the sliding window for each dimension of the input tensor.
    with tf.name_scope("Upscore"):
        W_upscore = tf.Variable(
            tf.truncated_normal([31, 31, 31, 2, 2], stddev=0.1, dtype=tf.float32),
            name="W_upscore",
        )
        print_tensor_shape(W_upscore, "W_upscore shape")
        # 		upscore_conv_op = tf.nn.conv3d_transpose( score_classes_conv_op, W_upscore,output_shape=[BATCH_SIZE,256,256,256,2],strides=[1,16,16,16,1],padding='SAME',name='upscore_conv_op')
        upscore_conv_op = tf.nn.conv3d_transpose(
            score_classes_conv_op,
            W_upscore,
            output_shape=[BATCH_SIZE, IMG_DIM, IMG_DIM, IMG_DIM, 2],
            strides=[1, 64, 64, 64, 1],
            padding="SAME",
            name="upscore_conv_op",
        )
        # 		upscore_conv_op = tf.nn.conv3d_transpose( score_classes_conv_op, W_upscore,output_shape=[1,256,256,256,2],strides=[1,64,64,64,1],padding='SAME',name='upscore_conv_op')
        print_tensor_shape(upscore_conv_op, "upscore_conv_op shape")

    return upscore_conv_op


def loss_fn(logits, labels):
    # input:  logits: Logits tensor, float - [batch_size, 256, 256, 256, 2].
    # intput: labels: Labels tensor, int8 - [batch_size, 256, 256, 256].
    # output: loss: Loss tensor of type float.

    labels = tf.to_int64(labels)
    print_tensor_shape(logits, "logits shape ")
    print_tensor_shape(labels, "labels shape ")

    # reshape to match args required for the cross entropy function
    logits_re = tf.reshape(logits, [-1, 2])
    labels_re = tf.reshape(labels, [-1])
    # print_tensor_shape( logits_re, 'logits shape after')
    # print_tensor_shape( labels_re, 'labels shape after')

    # call cross entropy with logits
    cross_entropy = tf.nn.sparse_softmax_cross_entropy_with_logits(
        logits=logits, labels=labels, name="cross_entropy"
    )
    print_tensor_shape(cross_entropy, "cross_entropy shape ")

    loss = tf.reduce_mean(cross_entropy, name="1cnn_cross_entropy_mean")
    print_tensor_shape(loss, "loss shape ")

    return loss


def training(loss, learning_rate, decay_steps, decay_rate):
    # input: loss: loss tensor from loss()
    # input: learning_rate: scalar for gradient descent
    # output: train_op the operation for training

    #    Creates a summarizer to track the loss over time in TensorBoard.
    #    Creates an optimizer and applies the gradients to all trainable variables.

    #    The Op returned by this function is what must be passed to the
    #    `sess.run()` call to cause the model to train.

    # Add a scalar summary for the snapshot loss.
    # tf.scalar_summary(loss.op.name, loss)
    tf.summary.scalar(loss.op.name, loss)

    # Create a variable to track the global step.
    global_step = tf.Variable(0, name="global_step", trainable=False)

    # create learning_decay
    lr = tf.train.exponential_decay(
        learning_rate, global_step, decay_steps, decay_rate, staircase=True
    )
    # tf.scalar_summary('1learning_rate', lr )
    tf.summary.scalar("learning_rate", lr)

    # Create the gradient descent optimizer with the given learning rate.
    #    optimizer = tf.train.GradientDescentOptimizer(learning_rate)
    optimizer = tf.train.GradientDescentOptimizer(lr)

    # Use the optimizer to apply the gradients that minimize the loss
    # (and also increment the global step counter) as a single training step.
    print_tensor_shape(loss, "loss shape ")
    train_op = optimizer.minimize(loss, global_step=global_step)

    return train_op


def evaluation(logits, labels):
    # input: logits: Logits tensor, float - [batch_size, 256, 256, 256, NUM_CLASSES].
    # input: labels: Labels tensor, int8 - [batch_size, 256, 256, 256]
    # output: scaler int32 tensor with number of examples that were
    #         predicted correctly

    with tf.name_scope("eval"):
        labels = tf.to_int64(labels)
        print_tensor_shape(logits, "logits eval shape before")
        print_tensor_shape(labels, "labels eval shape before")

        # reshape to match args required for the cross entropy function
        logits_re = tf.reshape(logits, [-1, 2])
        labels_re = tf.reshape(labels, [-1])
        print_tensor_shape(logits, "logits eval shape after")
        print_tensor_shape(labels, "labels eval shape after")

        # For a classifier model, we can use the in_top_k Op.
        # It returns a bool tensor with shape [batch_size] that is true for
        # the examples where the label is in the top k (here k=1)
        # of all logits for that example.
        # correct = tf.nn.in_top_k(logits_re, labels_re, 1)
        correct = tf.nn.in_top_k(logits_re, labels_re, 1)
        print_tensor_shape(correct, "correct shape")

        # Return the number of true entries.
        return tf.reduce_sum(tf.cast(correct, tf.int32))


################################################


def run_training():
    with tf.Graph().as_default():
        # specify the training data file location
        trainfile = os.path.join(INPUT_DIR, TRAIN_FILE)
        images, labels = inputs(
            train=True, batch_size=BATCH_SIZE, num_epochs=NUM_EPOCHS, filename=trainfile
        )
        # run inference on the images
        results = inference(images)
        # calculate the loss from the results of inference and the labels
        loss = loss_fn(results, labels)
        # setup the training operations
        train_op = training(loss, LEARNING_RATE, DECAY_STEPS, DECAY_RATE)
        # setup the summary ops to use TensorBoard
        summary_op = tf.summary.merge_all()
        # init to setup the initial values of the weights
        # init_op = tf.group(tf.initialize_all_variables(),tf.initialize_local_variables())
        init_op = tf.group(
            tf.global_variables_initializer(), tf.local_variables_initializer()
        )
        # setup a saver for saving checkpoints
        saver = tf.train.Saver()
        # create the session
        sess = tf.Session()
        # specify where to write the log files for import to TensorBoard
        summary_writer = tf.summary.FileWriter(CHECKPOINT_DIR, sess.graph)
        # initialize the graph
        sess.run(init_op)
        # setup the coordinato and threadsr.  Used for multiple threads to read data.
        # Not strictly required since we don't have a lot of data but typically
        # using multiple threads to read data improves performance
        coord = tf.train.Coordinator()
        threads = tf.train.start_queue_runners(sess=sess, coord=coord)
        # loop will continue until we run out of input training cases
        try:
            step = 0
            while not coord.should_stop():
                # start time and run one training iteration
                start_time = time.time()
                _, loss_value = sess.run([train_op, loss])
                duration = time.time() - start_time

                # print some output periodically
                if step % 100 == 0:
                    print(
                        "OUTPUT: Step %d: loss = %.3f (%.3f sec)"
                        % (step, loss_value, duration)
                    )
                    # output some data to the log files for tensorboard
                    summary_str = sess.run(summary_op)
                    summary_writer.add_summary(summary_str, step)
                    summary_writer.flush()
                # less frequently output checkpoint files.  Used for evaluating the model
                if step % 1000 == 0:
                    checkpoint_path = os.path.join(CHECKPOINT_DIR, "model.ckpt")
                    saver.save(sess, checkpoint_path, global_step=step)
                step += 1
        # quit after we run out of input files to read
        except tf.errors.OutOfRangeError:
            print("OUTPUT: Done training for %d epochs, %d steps." % (NUM_EPOCHS, step))
            checkpoint_path = os.path.join(CHECKPOINT_DIR, "model.ckpt")
            saver.save(sess, checkpoint_path, global_step=step)
        finally:
            coord.request_stop()
        # shut down the threads gracefully
        coord.join(threads)
        sess.close()


run_training()
