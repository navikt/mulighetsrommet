import { useOpplaringKategorisering } from "@/api/amo/useOpplaringKategorisering";
import {
  OpplaringKategoriseringResponseSeleksjonstype,
  Tiltakskode,
  Verdi,
  Verdigruppe,
} from "@tiltaksadministrasjon/api-client";
import { FormSelect } from "../skjema/FormSelect";
import { HGrid } from "@navikt/ds-react";
import { FieldValues, Path } from "react-hook-form";
import { FormComboboxMulti } from "../skjema/FormComboboxMulti";
import { SertifiseringerSkjema } from "./SertifiseringerSelect";

export function OpplaringKategoriseringForm({ tiltakskode }: { tiltakskode: Tiltakskode }) {
  const { data: kodeverk } = useOpplaringKategorisering(tiltakskode);
  return kodeverk.alternativer.map((container, index) => {
    switch (container.type) {
      case "Gruppe":
        return null;
      case "Verdigruppe":
        return <VerdigruppeVelger key={`container-${index}`} verdigruppe={container} />;
      case "VerdigruppeSok":
        return (
          <SertifiseringerSkjema path={`detaljer.amoKategorisering.${container.representerer}`} />
        );
      case undefined:
        throw Error("Ugyldig type container");
    }
  });
}

function VerdigruppeVelger({ verdigruppe }: { verdigruppe: Verdigruppe }) {
  switch (verdigruppe.seleksjonstype) {
    case OpplaringKategoriseringResponseSeleksjonstype.ENKELTVALG:
      return <VerdiGruppeEnkeltvalg verdigruppe={verdigruppe} />;
    case OpplaringKategoriseringResponseSeleksjonstype.FLERVALG:
      return <VerdiGruppeFlervalg verdigruppe={verdigruppe} />;
  }
}

function VerdiGruppeEnkeltvalg<T extends FieldValues>({
  verdigruppe,
}: {
  verdigruppe: Verdigruppe;
}) {
  return (
    <HGrid gap="space-16" columns={1}>
      <FormSelect<T>
        name={`detaljer.amoKategorisering.${verdigruppe.representerer}` as Path<T>}
        label={verdigruppe.visningsnavn}
      >
        <option key="0" value="">
          Velg kurstype
        </option>
        {verdigruppe.alternativer.map((verdi: Verdi) => (
          <option key={verdi.id} value={verdi.id}>
            {verdi.visningsnavn}
          </option>
        ))}
      </FormSelect>
    </HGrid>
  );
}

function VerdiGruppeFlervalg<T extends FieldValues>({ verdigruppe }: { verdigruppe: Verdigruppe }) {
  return (
    <FormComboboxMulti<T>
      label={verdigruppe.visningsnavn}
      placeholder={"Velg opptil flere"}
      name={`detaljer.amoKategorisering.${verdigruppe.representerer}` as Path<T>}
      options={verdigruppe.alternativer.map((verdi: Verdi) => ({
        value: verdi.id,
        label: verdi.visningsnavn,
      }))}
    />
  );
}
