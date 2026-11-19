import {
  OpplaeringtilskuddKode,
  PrismodellType,
  Tiltakskode,
  Valuta,
} from "@tiltaksadministrasjon/api-client";
import { useFormContext } from "react-hook-form";
import { BodyShort, Box, Checkbox, HStack, VStack } from "@navikt/ds-react";
import { formaterValutaBelop } from "@mr/frontend-common/utils/utils";
import { FormSelect } from "@/components/skjema/FormSelect";
import { FormTextarea } from "@/components/skjema/FormTextarea";
import { FormTextField } from "@/components/skjema/FormTextField";
import { FormCheckboxGroup } from "@/components/skjema/FormCheckboxGroup";
import { opplaeringTilskuddToString } from "@/utils/Utils";

export interface EnkeltplassPrisinformasjonValues {
  type: PrismodellType | "";
  totalbelop: string;
  prisbetingelser: string;
  tilskuddstyper?: OpplaeringtilskuddKode[];
  tilskudd?: Partial<Record<OpplaeringtilskuddKode, string>>;
}

export function EnkeltplassPrisinformasjonFelter({ tiltakskode }: { tiltakskode: Tiltakskode }) {
  const { watch } = useFormContext<EnkeltplassPrisinformasjonValues>();
  const type = watch("type");

  return (
    <>
      <FormSelect<EnkeltplassPrisinformasjonValues>
        name="type"
        label="Prismodell"
        rules={{ required: "Velg prismodell" }}
      >
        <option value="">Velg prismodell</option>
        {tiltakskode !== Tiltakskode.HOYERE_UTDANNING && (
          <option value={PrismodellType.ANSKAFFET_ENKELTPLASS}>Anskaffet enkeltplass</option>
        )}
        <option value={PrismodellType.TILSKUDD_TIL_OPPLAERING}>Tilskudd til opplæring</option>
        <option value={PrismodellType.INGEN_KOSTNADER}>Ingen kostnader</option>
      </FormSelect>
      {type === PrismodellType.ANSKAFFET_ENKELTPLASS && (
        <FormTextField<EnkeltplassPrisinformasjonValues>
          name="totalbelop"
          label="Totalbeløp (NOK)"
          type="number"
          min={1}
          step={1}
          rules={{
            required: "Oppgi totalbeløp",
            min: { value: 1, message: "Totalbeløpet må være minst 1 krone" },
          }}
        />
      )}
      {type === PrismodellType.TILSKUDD_TIL_OPPLAERING && <TilskuddPrisinformasjonFelter />}
      {type === PrismodellType.INGEN_KOSTNADER && (
        <FormTextarea<EnkeltplassPrisinformasjonValues>
          name="prisbetingelser"
          label="Begrunnelse"
          rules={{ required: "Oppgi begrunnelse" }}
        />
      )}
    </>
  );
}

function TilskuddPrisinformasjonFelter() {
  const { watch } = useFormContext<EnkeltplassPrisinformasjonValues>();
  const tilskuddstyper = watch("tilskuddstyper") ?? [];
  const tilskudd = watch("tilskudd");
  const totalbelop = tilskuddstyper.reduce((sum, type) => sum + (Number(tilskudd?.[type]) || 0), 0);

  return (
    <>
      <FormCheckboxGroup<EnkeltplassPrisinformasjonValues>
        name="tilskuddstyper"
        legend="Velg hvilke tilskudd som er aktuelle og anslå Navs utgifter"
        description="Hvis opplæringen går over flere semestre, skal du oppgi det estimerte totalbeløpet for hele perioden."
        size="small"
      >
        {[
          OpplaeringtilskuddKode.SEMESTERAVGIFT,
          OpplaeringtilskuddKode.EKSAMENSGEBYR,
          OpplaeringtilskuddKode.SKOLEPENGER,
          OpplaeringtilskuddKode.STUDIEREISE,
          OpplaeringtilskuddKode.INTEGRERT_BOTILBUD,
        ].map((type) => (
          <VStack key={type} gap="space-8">
            <Checkbox value={type}>{opplaeringTilskuddToString(type)}</Checkbox>
            {tilskuddstyper.includes(type) && (
              <Box paddingInline="space-32" paddingBlock="space-8">
                <FormTextField<EnkeltplassPrisinformasjonValues>
                  name={`tilskudd.${type}`}
                  label={`Estimert totalbeløp for ${opplaeringTilskuddToString(type).toLowerCase()} (NOK)`}
                  type="number"
                  min={1}
                  step={1}
                  htmlSize={12}
                  rules={{
                    required: "Oppgi estimert totalbeløp",
                    min: { value: 1, message: "Beløpet må være minst 1 krone" },
                    validate: (value) =>
                      Number.isInteger(Number(value)) || "Beløpet må være et heltall",
                  }}
                />
              </Box>
            )}
          </VStack>
        ))}
      </FormCheckboxGroup>
      <Box borderWidth="1 0 0 0" borderColor="neutral-subtle" paddingBlock="space-16">
        <HStack justify="space-between">
          <BodyShort weight="semibold">Estimert totalsum</BodyShort>
          <BodyShort weight="semibold">
            {formaterValutaBelop({ belop: totalbelop, valuta: Valuta.NOK })}
          </BodyShort>
        </HStack>
      </Box>
      <FormTextarea<EnkeltplassPrisinformasjonValues>
        name="prisbetingelser"
        label="Eventuell delfinansiering fra brukeren (valgfritt)"
        description="Ved full egenfinansiering skal du velge «Ingen kostnader» høyere opp."
        rules={{ required: false }}
      />
    </>
  );
}
