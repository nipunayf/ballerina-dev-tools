#!/bin/bash

# Take input from the user
[[ "$1" = "n" ]] && NAME="add_lock_negative" || NAME="add_lock"
PARENT_DIR="$(dirname "${BASH_SOURCE[0]}")"


# Check if the file with the pattern "$NAME[0-9]+" exists in the source directory
PREFIX_FILE="$PARENT_DIR/config/$NAME[0-9]*"
if ls $PREFIX_FILE 1> /dev/null 2>&1; then
    # Obtain the highest number from the existing files and increment it
    LAST_FILE=$(ls -v $PREFIX_FILE | sort -V | tail -n 1)
    NUMBER=$(echo "$LAST_FILE" | sed -n 's/.*'$NAME'\([0-9]*\)\.json/\1/p')
    echo $NUMBER
    ((NUMBER++))
else
    NUMBER=1
fi

# Create a bal with the incremented number
NEW_FILE="$NAME$NUMBER"
touch "$PARENT_DIR/source/$NEW_FILE.bal"

# Create a config file with the incremented number
if [ "$1" = "n" ]; then
    echo '{
  "position": {
    "line": 1,
    "character": 1
  },
  "source": "'$NEW_FILE.bal'",
  "expected": [
    {
      "title": "Surround with lock"
    }
  ]
}' > "$PARENT_DIR/config/$NEW_FILE.json"
else
    echo '{
  "position": {
    "line": 1,
    "character": 1
  },
  "source": "'$NEW_FILE.bal'",
  "description": "Wrap in a lock statement for a ",
  "expected": [
    {
      "title": "Surround with lock",
      "kind": "quickfix",
      "edits": []
    }
  ]
}' > "$PARENT_DIR/config/$NEW_FILE.json"
fi