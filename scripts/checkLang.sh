#!/bin/bash
grep '\".*\"' * -R | egrep -v 'localName\.|Log\.|scripts/|\://|\.xml:|gpl.txt|jcoord/|/db/| TAG = |PARCELABLE|KEY_|getText\(' | grep --color '\".*\"'
grep '\".*\"' * -R | egrep -v 'localName\.|Log\.|scripts/|\://|\.xml:|gpl.txt|jcoord/|/db/| TAG = |PARCELABLE|KEY_|getText\(' | grep '\".*\"' | wc -l
