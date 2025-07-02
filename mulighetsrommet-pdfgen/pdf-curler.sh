#!/bin/bash
#
# List ut mock objekter og kall pdfgen med rett endepunkt avhengig av filtypen
# <endepunkt-filnavn>

files=()
for f in data/utbetaling/*.json; do
  files+=("$f")
done

unique_files=($(printf "%s\n" "${files[@]}" | sort -u))

echo "Velg fil:"
select fname in "${unique_files[@]}"; do
  if [[ -n "$fname" ]]; then
    file="$fname"
    break
  else
    echo "Ugyldig valg"
  fi
done

filename="$(basename "$file")"
name="${filename%.*}"

curl --header "Content-Type: application/json" \
  --request POST \
  --data @"$file" \
  "http://localhost:8888/api/v1/genpdf/utbetaling/${name%%-*}" \
  --output "$name.pdf"

printf "\nPdf generert: $name.pdf\n"
