import { useOpplaringKategorisering } from "@/api/amo/useOpplaringKategorisering";
import {
  Container,
  Gruppe,
  OpplaringKategoriseringResponseSeleksjonstype,
  OpplaringKategoriseringResponseTooltip,
  Tiltakskode,
  Utlisting,
  Verdi,
  Verdigruppe,
} from "@tiltaksadministrasjon/api-client";
import { FormSelect } from "../skjema/FormSelect";
import { Box, Heading, HGrid, List } from "@navikt/ds-react";
import { FieldValues, Path, useFormContext } from "react-hook-form";
import { FormComboboxMulti } from "../skjema/FormComboboxMulti";
import { SertifiseringerSkjema } from "./SertifiseringerSelect";
import { useMemo } from "react";
import { LabelWithHelpText } from "@mr/frontend-common/components/label/LabelWithHelpText";

export function OpplaringKategoriseringForm({ tiltakskode }: { tiltakskode: Tiltakskode }) {
  const { data: kodeverk } = useOpplaringKategorisering(tiltakskode);
  return kodeverk.alternativer.map((container, index) => (
    <ContainerVelger key={`container-${index}`} container={container} />
  ));
}

function getPath<T extends FieldValues>(propName: string | null): Path<T> {
  return `detaljer.amoKategorisering.${propName}` as Path<T>;
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
    return <GruppeVerdiVelger gruppe={gruppe} />;
  }
  return null;
}

function GruppeVerdiVelger<T extends FieldValues>({ gruppe }: { gruppe: Gruppe }) {
  const { watch, resetField } = useFormContext<T>();
  const name = getPath<T>(gruppe.representerer);
  const valgtGruppe = watch(name);
  const undervalg: Container[] = useMemo(() => {
    const valgtUndergruppe: Container | null =
      gruppe.alternativer.find((c: Container) => c.id === valgtGruppe) ?? null;
    switch (valgtUndergruppe?.type) {
      case "Gruppe": {
        valgtUndergruppe.alternativer.forEach((refName) =>
          resetField(getPath<T>(refName.representerer)),
        );
        return valgtUndergruppe.alternativer;
      }
      case "Verdigruppe":
      case "VerdigruppeSok": {
        resetField(getPath<T>(valgtUndergruppe.representerer));
        return [valgtUndergruppe];
      }
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
  const label = verdigruppe.tooltip ? (
    <LabelWithHelpText label={verdigruppe.visningsnavn}>
      <TooltipVelger tooltip={verdigruppe.tooltip} />
    </LabelWithHelpText>
  ) : (
    verdigruppe.visningsnavn
  );
  return (
    <HGrid gap="space-16" columns={1}>
      <FormSelect<T>
        name={`detaljer.amoKategorisering.${verdigruppe.representerer}` as Path<T>}
        label={label}
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

function TooltipVelger({ tooltip }: { tooltip: OpplaringKategoriseringResponseTooltip }) {
  switch (tooltip.type) {
    case "Utlisting":
      return <TooltipUtlisting utlisting={tooltip} />;
    case "FlereUtlistinger":
      return (
        <div
          style={{
            maxHeight: "400px",
            overflow: "auto",
          }}
        >
          {tooltip.liste.map((utlisting, index) => {
            return <TooltipUtlisting key={`tooltip-${index}`} utlisting={utlisting} />;
          })}
        </div>
      );
    case undefined:
      return null;
  }
}

interface TooltipUtlistingProps {
  utlisting: Utlisting;
}

function TooltipUtlisting({ utlisting }: TooltipUtlistingProps) {
  return (
    <div>
      <Heading as="h3" size="xsmall">
        {utlisting.header}
      </Heading>
      <Box marginBlock="space-12" asChild>
        <List data-aksel-migrated-v8 as="ul" size="small">
          {utlisting.items.map((item) => (
            <List.Item key={item}>{item}</List.Item>
          ))}
        </List>
      </Box>
    </div>
  );
}
