#!/bin/bash

# should be runned on vps

DB_DIR="$HOME/.legomenon"

mkdir -p "$DB_DIR/backup"
cp "$DB_DIR/database.db" "$DB_DIR/backup/$(date -u +%Y-%m-%dT%H:%M:%S)"
