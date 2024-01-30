# From: https://github.com/ponder-lab/samples/blob/39f7644391e664244b45c90868c804abad923eb3/tensorflow_gan_tutorial/tensorflow_gan_tutorial.py

#!/usr/bin/env python

import os
import time
import random
import matplotlib.pyplot as plt

import numpy as np
import tensorflow as tf


def make_generator_model():
    input_node = tf.keras.Input((100,))
    x = input_node
    x = tf.keras.layers.Dense(7 * 7 * 256, use_bias=False)(x)
    x = tf.keras.layers.BatchNormalization()(x)
    x = tf.keras.layers.LeakyReLU()(x)
    x = tf.keras.layers.Reshape((7, 7, 256))(x)
    x = tf.keras.layers.Conv2DTranspose(
        128, (5, 5), strides=(1, 1), padding="same", use_bias=False
    )(x)
    x = tf.keras.layers.BatchNormalization()(x)
    x = tf.keras.layers.LeakyReLU()(x)
    x = tf.keras.layers.Conv2DTranspose(
        64, (5, 5), strides=(2, 2), padding="same", use_bias=False
    )(x)
    x = tf.keras.layers.BatchNormalization()(x)
    x = tf.keras.layers.LeakyReLU()(x)
    x = tf.keras.layers.Conv2DTranspose(
        1, (5, 5), strides=(2, 2), padding="same", use_bias=False
    )(x)
    output_node = x

    return tf.keras.models.Model(input_node, output_node)


def make_discriminator_model():
    input_node = tf.keras.Input((28, 28, 1))
    x = input_node
    x = tf.keras.layers.Conv2D(64, (5, 5), strides=(2, 2), padding="same")(x)
    x = tf.keras.layers.LeakyReLU()(x)
    x = tf.keras.layers.Dropout(0.3)(x)
    x = tf.keras.layers.Conv2D(128, (5, 5), strides=(2, 2), padding="same")(x)
    x = tf.keras.layers.LeakyReLU()(x)
    x = tf.keras.layers.Dropout(0.3)(x)
    x = tf.keras.layers.Flatten()(x)
    x = tf.keras.layers.Dense(1)(x)
    output_node = x

    return tf.keras.models.Model(input_node, output_node)


CROSS_ENTROPY = tf.keras.losses.BinaryCrossentropy(from_logits=True)


def discriminator_loss(real_output, fake_output):
    real_loss = CROSS_ENTROPY(tf.ones_like(real_output), real_output)
    fake_loss = CROSS_ENTROPY(tf.zeros_like(fake_output), fake_output)
    total_loss = real_loss + fake_loss
    return total_loss


def generator_loss(fake_output):
    return CROSS_ENTROPY(tf.ones_like(fake_output), fake_output)


def train_step(
    images, generator, discriminator, generator_optimizer, discriminator_optimizer
):
    noise = tf.random.normal([images.shape[0], noise_dim])

    with tf.GradientTape() as gen_tape, tf.GradientTape() as disc_tape:
        generated_images = generator(noise, training=True)

        real_output = discriminator(images, training=True)
        fake_output = discriminator(generated_images, training=True)

        gen_loss = generator_loss(fake_output)
        disc_loss = discriminator_loss(real_output, fake_output)

    gen_grads = gen_tape.gradient(gen_loss, generator.trainable_variables)
    disc_grads = disc_tape.gradient(disc_loss, discriminator.trainable_variables)

    generator_optimizer.apply_gradients(zip(gen_grads, generator.trainable_variables))
    discriminator_optimizer.apply_gradients(
        zip(disc_grads, discriminator.trainable_variables)
    )


def train(
    dataset,
    epochs,
    checkpoint,
    generator,
    discriminator,
    generator_optimizer,
    discriminator_optimizer,
    seed,
):
    # for epoch in range(epochs, epochs*2):
    for epoch in range(epochs):
        start = time.time()

        for image_batch in dataset:
            train_step(
                image_batch,
                generator,
                discriminator,
                generator_optimizer,
                discriminator_optimizer,
            )

        generate_and_save_images(generator, epoch + 1, seed)

        if (epoch + 1) % 15 == 0:
            checkpoint_dir = "./training_checkpoints"
            checkpoint_prefix = os.path.join(checkpoint_dir, "ckpt")
            checkpoint.save(file_prefix=checkpoint_prefix)

        print("Time for epoch {} is {} sec".format(epoch + 1, time.time() - start))

    generate_and_save_images(generator, epochs, seed)


def generate_and_save_images(model, epoch, test_input):
    predictions = model(test_input, training=False)

    fig = plt.figure(figsize=(4, 4))

    for i in range(predictions.shape[0]):
        plt.subplot(4, 4, i + 1)
        plt.imshow(predictions[i, ..., 0] * 127.5 + 127.5, cmap="gray")
        plt.axis("off")

    plt.savefig("image_at_epoch_{:04d}.png".format(epoch))
    plt.close()


random.seed(0)
np.random.seed(0)
tf.random.set_seed(0)

(train_images, train_labels), (_, _) = tf.keras.datasets.mnist.load_data()

train_images = train_images[..., None].astype(np.float32)
train_images = (train_images - 127.5) / 127.5

buffer_size = train_images.shape[0]
batch_size = 256

train_dataset = (
    tf.data.Dataset.from_tensor_slices(train_images)
    .shuffle(buffer_size)
    .batch(batch_size)
)

generator = make_generator_model()
# generator.summary()

# noise = tf.random.normal([1, 100])
# generated_image = generator(noise, training=False)

# plt.imshow(generated_image[0, ..., 0], cmap="gray")
# plt.show()

discriminator = make_discriminator_model()
# discriminator.summary()

# decision = discriminator(generated_image)
# print(decision)

generator_optimizer = tf.keras.optimizers.Adam(1e-4)
discriminator_optimizer = tf.keras.optimizers.Adam(1e-4)

checkpoint = tf.train.Checkpoint(
    generator_optimizer=generator_optimizer,
    discriminator_optimizer=discriminator_optimizer,
    generator=generator,
    discriminator=discriminator,
)

# checkpoint_dir = "./training_checkpoints"
# checkpoint.restore(tf.train.latest_checkpoint(checkpoint_dir))

epochs = 50
noise_dim = 100
num_examples_to_generate = 16

seed = tf.random.normal([num_examples_to_generate, noise_dim])

train(
    train_dataset,
    epochs,
    checkpoint,
    generator,
    discriminator,
    generator_optimizer,
    discriminator_optimizer,
    seed,
)
