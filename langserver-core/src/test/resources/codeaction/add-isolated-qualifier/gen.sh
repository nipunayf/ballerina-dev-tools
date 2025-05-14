#!/bin/bash

# Take input from the user
[[ "$1" = "n" ]] && NAME="add_isolated_qualifier_negative_config" || NAME="add_isolated_qualifier_config"
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

# Create a config file with the incremented number
if [ "$1" = "n" ]; then
    echo '{
  "position": {
    "line": 1,
    "character": 1
  },
  "source": "isolatedFunctionCodeAction/modules/module2/add_isolated_qualifier_source6.bal",
  "expected": [
    {
      "title": "Add isolated qualifier to %s"
    }
  ]
}' > "$PARENT_DIR/config/$NEW_FILE.json"
else
    echo '{
  "position": {
    "line": 1,
    "character": 1
  },
  "source": "isolatedFunctionCodeAction/modules/module2/add_isolated_qualifier_source6.bal",
  "description": "Add isolated qualifier to a variable",
  "expected": [
    {
      "title": "Add isolated qualifier to %s",
      "kind": "quickfix",
      "edits": []
    }
  ]
}' > "$PARENT_DIR/config/$NEW_FILE.json"
fi