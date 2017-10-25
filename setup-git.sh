#!/bin/sh

# Copy the pre-commit hook that automatically
# includes the updated jar in each commit
cd $(git rev-parse --show-toplevel)
cp .pre-commit-hook .git/hooks/pre-commit
