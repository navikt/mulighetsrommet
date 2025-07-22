#!/bin/bash
#
# List ut mock objekter og kall pdfgen med rett endepunkt avhengig av valgt template og data.

template_files=()
for f in templates/*/*.hbs; do
  template_files+=("$f")
done

if [ ${#template_files[@]} -eq 1 ]; then
  template_file="${template_files[0]}"
else
  echo "Velg template:"
  select fname in "${template_files[@]}"; do
    if [[ -n "$fname" ]]; then
      template_file="$fname"
      break
    else
      echo "Ugyldig valg"
    fi
  done
fi

echo "Valgt template: $template_file"

data_files=()
for f in data/*/*.json; do
  data_files+=("$f")
done

if [ ${#data_files[@]} -eq 1 ]; then
  data_file="${data_files[0]}"
else
  echo -e "\nVelg data:"
  select fname in "${data_files[@]}"; do
    if [[ -n "$fname" ]]; then
      data_file="$fname"
      break
    else
      echo "Ugyldig valg"
    fi
  done
fi

echo "Valgt data: $data_file"

app=$(basename $(dirname "$template_file"))
template=$(basename "$template_file" .hbs)

echo ""
echo "Genererer PDF: app=$app, template=$template"

# Run curl in silent mode but show errors if they occur
curl -s -S --fail \
  --header "Content-Type: application/json" \
  --request POST \
  --data @"$data_file" \
  "http://localhost:8888/api/v1/genpdf/$app/$template" \
  --output "$template.pdf"

# Check curl exit status
if [ $? -eq 0 ]; then
  echo -e "\nGenerert PDF: $template.pdf"
else
  echo -e "\nFeilet å generere PDF. Se feilmelding ovenfor."
fi
