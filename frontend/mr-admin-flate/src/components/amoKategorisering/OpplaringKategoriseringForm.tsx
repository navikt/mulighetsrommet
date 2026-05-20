import { useOpplaringKategorisering } from "@/api/amo/useOpplaringKategorisering";
import {
  Container,
  Gruppe,
  OpplaringKategoriseringResponseSeleksjonstype,
  Tiltakskode,
  Verdi,
  Verdigruppe,
} from "@tiltaksadministrasjon/api-client";
import { FormSelect } from "../skjema/FormSelect";
import { HGrid } from "@navikt/ds-react";
import { FieldValues, Path, useFormContext } from "react-hook-form";
import { FormComboboxMulti } from "../skjema/FormComboboxMulti";
import { SertifiseringerSkjema } from "./SertifiseringerSelect";
import { useMemo } from "react";

export function OpplaringKategoriseringForm({ tiltakskode }: { tiltakskode: Tiltakskode }) {
  const { data: kodeverk } = useOpplaringKategorisering(tiltakskode);
  return kodeverk.alternativer.map((container, index) => (
    <ContainerVelger key={`container-${index}`} container={container} />
  ));
}

function ContainerVelger({ container }: { container: Container }) {
  switch (container.type) {
    case "Gruppe":
      return <GruppeVelger gruppe={container} />;
    case "Verdigruppe":
      return <VerdigruppeVelger verdigruppe={container} />;
    case "VerdigruppeSok":
      return (
        <SertifiseringerSkjema path={`detaljer.amoKategorisering.${container.representerer}`} />
      );
    case undefined:
      throw Error("Ugyldig type container");
  }
}

function GruppeVelger({ gruppe }: { gruppe: Gruppe }) {
  if (!gruppe.id && gruppe.representerer) {
    <GruppeVerdiVelger gruppe={gruppe} />;
  }
  return null;
}

function GruppeVerdiVelger<T extends FieldValues>({ gruppe }: { gruppe: Gruppe }) {
  const { watch } = useFormContext<T>();
  const name = `detaljer.amoKategorisering.${gruppe.representerer}` as Path<T>;
  const valgtGruppe = watch(name);
  const undervalg: Container[] = useMemo(() => {
    const valgtUndergruppe: Container | undefined | null = gruppe.alternativer.find(
      (c: Container) => c.id === valgtGruppe,
    );
    switch (valgtUndergruppe?.type) {
      case "Gruppe":
        return valgtUndergruppe.alternativer;
      case "Verdigruppe":
      case "VerdigruppeSok":
        return [valgtUndergruppe];
      case undefined:
        return [];
    }
  }, [valgtGruppe]);

  return (
    <>
      <FormSelect<T> name={name} label={gruppe.visningsnavn} required={gruppe.required}>
        <option value="">Velg en</option>
        {gruppe.alternativer.map((container) => (
          <option key={container.id} value={container.id?.toString()}>
            {container.visningsnavn}
          </option>
        ))}
      </FormSelect>
      {undervalg.map((c, index) => (
        <ContainerVelger key={`container-${index}`} container={c} />
      ))}
    </>
  );
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
        required={verdigruppe.required}
      >
        <option value="">Velg en</option>
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
      required={verdigruppe.required}
      options={verdigruppe.alternativer.map((verdi: Verdi) => ({
        value: verdi.id,
        label: verdi.visningsnavn,
      }))}
    />
  );
}
