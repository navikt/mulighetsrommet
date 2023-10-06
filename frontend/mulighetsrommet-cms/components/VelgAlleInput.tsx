import { Grid, Stack, Button } from "@sanity/ui";
import { ArrayOfObjectsInputProps, ObjectInputProps, useClient } from "sanity";

export function VelgAlleInput(props: ObjectInputProps, melding: any) {
  const { onChange } = props;
  const client = useClient({ apiVersion: "2023-06-10" });

  // TODO Må få inn dokumentet så vi vet alle enhetene til fylket valgt

  function handleClick() {
    console.log(props);
  }

  return (
    <Stack space={3}>
      {props.renderDefault(props)}
      <Grid>
        <Button onClick={handleClick}>Velg alle enheter</Button>
      </Grid>
    </Stack>
  );
}
