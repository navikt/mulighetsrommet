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

interface Props<T> {
  tiltakskode: Tiltakskode;
  basePath: Path<T>;
}
export function OpplaringKategoriseringForm<T extends FieldValues>({
  tiltakskode,
  basePath,
}: Props<T>) {
  const { data: kodeverk } = useOpplaringKategorisering(tiltakskode);
  return kodeverk.alternativer.map((container, index) => (
    <ContainerVelger key={`container-${index}`} container={container} basePath={basePath} />
  ));
}

function getPath<T extends FieldValues>(base: Path<T>, propName: string | null): Path<T> {
  return `${base}.${propName}` as Path<T>;
}

function ContainerVelger<T>({ container, basePath }: { container: Container; basePath: Path<T> }) {
  switch (container.type) {
    case "Gruppe":
      return <GruppeVelger gruppe={container} basePath={basePath} />;
    case "Verdigruppe":
      return <VerdigruppeVelger verdigruppe={container} basePath={basePath} />;
    case "VerdigruppeSok":
      return <SertifiseringerSkjema path={`${basePath}.${container.representerer}`} />;
    case undefined:
      throw Error("Ugyldig type container");
  }
}

function GruppeVelger<T>({ gruppe, basePath }: { gruppe: Gruppe; basePath: Path<T> }) {
  if (!gruppe.id && gruppe.representerer) {
    return <GruppeVerdiVelger gruppe={gruppe} basePath={basePath} />;
  }
  return null;
}

function GruppeVerdiVelger<T extends FieldValues>({
  gruppe,
  basePath,
}: {
  gruppe: Gruppe;
  basePath: Path<T>;
}) {
  const { watch, getValues, resetField } = useFormContext<T>();
  const name = getPath<T>(basePath, gruppe.representerer);
  const valgtGruppe = watch(name);
  const undervalg: Container[] = useMemo(() => {
    const valgtUndergruppe: Container | null =
      gruppe.alternativer.find((c: Container) => c.id === valgtGruppe) ?? null;
    switch (valgtUndergruppe?.type) {
      case "Gruppe": {
        valgtUndergruppe.alternativer.forEach((alt) => {
          const altName = getPath<T>(basePath, alt.representerer);
          const altValues = getValues(altName);
          resetField(altName, { defaultValue: Array.isArray(altValues) ? [] : "" });
        });
        return valgtUndergruppe.alternativer;
      }
      case "Verdigruppe":
      case "VerdigruppeSok": {
        resetField(getPath<T>(basePath, valgtUndergruppe.representerer), {
          defaultValue: undefined,
        });
        return [valgtUndergruppe];
      }
      case undefined:
        return [];
    }
  }, [valgtGruppe]);

  const rules = gruppe.required ? { required: true } : {};

  return (
    <>
      <FormSelect<T> name={name} label={gruppe.visningsnavn} rules={rules}>
        <option value="">Velg {gruppe.visningsnavn.toLowerCase()}</option>
        {gruppe.alternativer.map((container) => (
          <option key={container.id} value={container.id?.toString()}>
            {container.visningsnavn}
          </option>
        ))}
      </FormSelect>
      {undervalg.map((c, index) => (
        <ContainerVelger key={`container-${index}`} container={c} basePath={basePath} />
      ))}
    </>
  );
}

function VerdigruppeVelger<T>({
  verdigruppe,
  basePath,
}: {
  verdigruppe: Verdigruppe;
  basePath: Path<T>;
}) {
  switch (verdigruppe.seleksjonstype) {
    case OpplaringKategoriseringResponseSeleksjonstype.ENKELTVALG:
      return <VerdiGruppeEnkeltvalg verdigruppe={verdigruppe} basePath={basePath} />;
    case OpplaringKategoriseringResponseSeleksjonstype.FLERVALG:
      return <VerdiGruppeFlervalg verdigruppe={verdigruppe} basePath={basePath} />;
  }
}

function VerdiGruppeEnkeltvalg<T extends FieldValues>({
  verdigruppe,
  basePath,
}: {
  verdigruppe: Verdigruppe;
  basePath: Path<T>;
}) {
  const label = verdigruppe.tooltip ? (
    <LabelWithHelpText label={verdigruppe.visningsnavn}>
      <TooltipVelger tooltip={verdigruppe.tooltip} />
    </LabelWithHelpText>
  ) : (
    verdigruppe.visningsnavn
  );
  const rules = verdigruppe.required ? { required: true } : {};
  return (
    <HGrid gap="space-16" columns={1}>
      <FormSelect<T>
        name={`${basePath}.${verdigruppe.representerer}` as Path<T>}
        label={label}
        rules={rules}
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

function VerdiGruppeFlervalg<T extends FieldValues>({
  verdigruppe,
  basePath,
}: {
  verdigruppe: Verdigruppe;
  basePath: Path<T>;
}) {
  const rules = verdigruppe.required ? { required: true } : {};
  return (
    <FormComboboxMulti<T>
      label={verdigruppe.visningsnavn}
      placeholder={"Velg opptil flere"}
      name={`${basePath}.${verdigruppe.representerer}` as Path<T>}
      rules={rules}
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
