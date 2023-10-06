import { Button, Grid, Stack } from "@sanity/ui";
import { randomKey } from "@sanity/util/content";
import { ObjectInputProps, Reference, set, useClient, useFormValue } from "sanity";
import { API_VERSION } from "../sanity.config";

export function VelgAlleEnheterForKontaktpersoner(props: ObjectInputProps) {
  const { onChange } = props;
  const client = useClient({ apiVersion: API_VERSION });

  // Hent ut valgt fylke i skjema
  const fylkeValgt = useFormValue(["fylke"]) as Reference;
  // Hent ut valgt kontaktperson som trengs ved patching av objekt
  const kontaktpersonValgt = useFormValue(props.path) as { navKontaktperson: Reference };

  async function handleClick() {
    const fylke = fylkeValgt?._ref;
    // Hent enhetene koblet til fylke
    const query = `*[_type == "enhet" && fylke._ref == $fylke]._id`;
    const enheter: string[] = (await client.fetch(query, { fylke })) || [];
    if (enheter.length === 0) alert("Fant ingen enheter for valgt fylke");

    // Opprett en Patch via set-metoden
    const data = set({
      _key: randomKey(12),
      navKontaktperson: {
        ...kontaktpersonValgt.navKontaktperson,
      },
      enheter: enheter.map((enhetsnummer) => {
        return {
          _key: randomKey(12),
          _type: "reference",
          _ref: enhetsnummer,
        };
      }),
    });

    // Endre dokumentet for å få med seg endringene
    onChange(data);
  }

  return (
    <Stack space={3}>
      {props.renderDefault(props)}
      <Grid>
        {!fylkeValgt ? (
          <p>Du må sette fylke før man kan velge alle enheter</p>
        ) : (
          <Button mode="ghost" onClick={handleClick}>
            Velg alle enheter
          </Button>
        )}
      </Grid>
    </Stack>
  );
}
