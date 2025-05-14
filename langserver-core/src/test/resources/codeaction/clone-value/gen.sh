#!/bin/bash

# Take input from the user
CODE_ACTION="clone_value"
[[ "$1" = "n" ]] && NAME="${CODE_ACTION}_negative_config" || NAME="${CODE_ACTION}_config"
PARENT_DIR="$(dirname "${BASH_SOURCE[0]}")"

# Check if the file with the pattern "$NAME[0-9]+" exists in the source directory
PREFIX_FILE="$PARENT_DIR/config/$NAME[0-9]*"
if ls $PREFIX_FILE 1> /dev/null 2>&1; then
    # Obtain the highest number from the existing files and increment it
    LAST_FILE=$(ls -v $PREFIX_FILE | sort -V | tail -n 1)
    NUMBER=$(echo "$LAST_FILE" | sed -n 's/.*'$NAME'\([0-9]*\)\.json/\1/p')
    NEW_NUMBER=$((NUMBER + 1))
else
    NEW_NUMBER=1
fi

# Create a bal with the incremented number
NEW_FILE="$NAME$NEW_NUMBER"

# Capture the latest file number in the source directory
LAST_FILE=$(ls -v $PARENT_DIR/source/$CODE_ACTION[0-9]* | sort -V | tail -n 1 | xargs -I {} basename {} | awk -F/ '{print $NF}')

# Capture the line and character from the JSON file
LINE=$(jq '.position.line' "$PARENT_DIR/config/$NAME$NUMBER.json" 2>/dev/null)
CHARACTER=$(jq '.position.character' "$PARENT_DIR/config/$NAME$NUMBER.json" 2>/dev/null)
DESCRIPTION=$(jq '.description' "$PARENT_DIR/config/$NAME$NUMBER.json" 2>/dev/null)

# Create a config file with the incremented number
if [ "$1" = "n" ]; then
    echo '{
  "position": {
    "line": '$LINE',
    "character": '$CHARACTER'
  },
  "source": "'$LAST_FILE'",
  "expected": [
    {
      "title": "Clone the value"
    },
    {
      "title": "Clone as a readonly value"
    }
  ]
}' > "$PARENT_DIR/config/$NEW_FILE.json"
else
    echo '{
  "position": {
    "line": '$LINE',
    "character": '$CHARACTER'
  },
  "source": "'$LAST_FILE'",
  "description": '$DESCRIPTION',
  "expected": [
    {
      "title": "Clone the value",
      "kind": "quickfix",
      "edits": []
    },
    {
      "title": "Clone as a readonly value",
      "kind": "quickfix",
      "edits": []
    }
  ]
}' > "$PARENT_DIR/config/$NEW_FILE.json"
fi
