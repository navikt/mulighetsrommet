#!/bin/bash
#
# Kall pdfgen med rett endepunkt avhengig av valgt template.
# Hver template har et definert sett med gyldige testdata slik at kun
# testdata som er ment for en gitt type kan velges.

# Velg template
template_files=()
shopt -s nullglob
for f in templates/block-content/*.typ; do
  template_files+=("$f")
done
shopt -u nullglob

if [ ${#template_files[@]} -eq 0 ]; then
  echo "Fant ingen templates (*.typ) i templates/."
  exit 1
fi

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

app=$(basename "$(dirname "$template_file")")
template=$(basename "$template_file" .typ)

echo "Valgt template: $template_file"

# Gyldige testdata bestemmes av mappestrukturen: data/<app>/<template>/*.json
data_dir="data/$app/$template"

data_files=()
shopt -s nullglob
for f in "$data_dir"/*.json; do
  data_files+=("$f")
done
shopt -u nullglob

if [ ${#data_files[@]} -eq 0 ]; then
  echo "Fant ingen testdata for template '$template' i $data_dir/."
  exit 1
fi

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

output_file="$(basename "$data_file" .json).pdf"

echo ""
echo "Genererer PDF: app=$app, template=$template"

# Run curl in silent mode but show errors if they occur
curl -s -S --fail \
  --header "Content-Type: application/json" \
  --request POST \
  --data @"$data_file" \
  "http://localhost:8888/api/v1/genpdf/$app/$template" \
  --output "$output_file"

# Check curl exit status
if [ $? -eq 0 ]; then
  echo -e "\nGenerert PDF: $output_file"
else
  echo -e "\nFeilet å generere PDF. Se feilmelding ovenfor."
fi
