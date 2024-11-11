# From https://github.com/akanyaani/gpt-2-tensorflow2.0/blob/ab93122396a02bfd76530ac6b414a525970bba96/train_gpt2.py.

import click


@click.command()
@click.option(
    "--data-dir",
    type=str,
    default="./data/scraped",
    show_default=True,
    help="training data path",
)
@click.option(
    "--vocab-size",
    type=int,
    default=24512,
    show_default=True,
    help="byte pair vocab size",
)
@click.option(
    "--min-seq-len",
    type=int,
    default=15,
    show_default=True,
    help="minimum sequence length",
)
@click.option(
    "--max-seq-len",
    type=int,
    default=512,
    show_default=True,
    help="minimum sequence length",
)
def train(data_dir, vocab_size, min_seq_len, max_seq_len):
    pass


if __name__ == "__main__":
    train()
